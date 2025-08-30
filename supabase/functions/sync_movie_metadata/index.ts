/// <reference lib="dom" />
// Deno Deploy Edge Function: sync_movie_metadata
// Purpose: Enrich Supabase movies with TMDB metadata and persist into the movies table.
// Routes:
//   POST /          -> sync one by { id? title? force? }
//   POST /bulk      -> bulk sync by { limit?: number, missingOnly?: boolean }
// Auth:
//   - Protected by X-Admin-Token header (ADMIN_TOKEN secret)
//   - Uses SERVICE_ROLE key to bypass RLS for writes
// Env vars required:
//   SUPABASE_URL
//   SUPABASE_SERVICE_ROLE_KEY
//   TMDB_API_KEY
//   ADMIN_TOKEN

// @ts-ignore: esm.sh import resolution in local IDE; valid in Deno runtime
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// Local IDE type hint: Deno is available at runtime; this avoids TS errors in Node tooling
// while keeping Deno.serve and Deno.env usages intact.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
declare const Deno: any;

const SUPABASE_URL =
  Deno.env.get("SUPABASE_URL") ||
  Deno.env.get("SB_URL") ||
  Deno.env.get("SUPABASE_PROJECT_URL");
// Some environments disallow setting secrets that start with SUPABASE_. Support fallbacks.
const SERVICE_ROLE =
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ||
  Deno.env.get("SERVICE_ROLE") ||
  Deno.env.get("SB_SERVICE_ROLE_KEY");
const TMDB_API_KEY = Deno.env.get("TMDB_API_KEY");
const ADMIN_TOKEN = Deno.env.get("ADMIN_TOKEN");

if (!SUPABASE_URL || !SERVICE_ROLE || !TMDB_API_KEY || !ADMIN_TOKEN) {
  console.error("Missing required environment variables for sync_movie_metadata function.");
}

const supabase = createClient(SUPABASE_URL!, SERVICE_ROLE!);

type MovieRow = {
  id: string;
  title: string;
  tmdb_id?: number | null;
};

type SyncPayload = {
  id?: string;
  title?: string;
  force?: boolean;
};

type BulkPayload = {
  limit?: number;
  missingOnly?: boolean;
};

function normalizeTitle(t: string) {
  return t.toLowerCase().replace(/[^a-z0-9]+/g, " ").trim();
}

function stripParentheticals(t: string) {
  // Remove content in parentheses and brackets and extra spaces
  return t.replace(/\s*[\(\[].*?[\)\]]\s*/g, " ").replace(/\s{2,}/g, " ").trim();
}

// Heuristic cleaner for display titles: removes noisy prefixes, trailer words, and trailing years
function cleanTitleForDisplay(t: string): string {
  let s = (t || "").trim();
  const original = s;

  // Normalize common separators from release names to spaces (e.g., "Movie.Title.2021.1080p")
  s = s.replace(/[._+]+/g, " ");

  // Remove multiple leading/trailing bracketed tags like [HD], (1080p), {WEBRip}
  for (let i = 0; i < 3; i++) s = s.replace(/^\s*(?:\[[^\]]+\]|\([^\)]+\)|\{[^\}]+\})\s*/gi, "").trim();
  for (let i = 0; i < 3; i++) s = s.replace(/\s*(?:\[[^\]]+\]|\([^\)]+\)|\{[^\}]+\})\s*$/gi, "").trim();

  // Remove common quality/source/filetype/release-group tokens
  const TOKS = "HD|FULL|4K|8K|2160P|1080P|720P|480P|WEB ?DL|WEB ?RIP|HDRIP|BRRIP|DVDRIP|BLURAY|BDRIP|CAM|HDCAM|TS|TELESYNC|REMUX|PROPER|REPACK|X264|X265|H264|HEVC|AAC|AAC5\\.1|DDP5\\.1|DD5\\.1|AC3|DTS|DTSHD|ATMOS|YTS(?:\\.MX)?|YIFY|RARBG|EVO|FGT|ETRG|MP4|MKV|AVI|MOV|WMV|MPEG|MPG|M4V";
  const startRe = new RegExp(`^\\s*(?:${TOKS})\\b\\s*(?:[-–—:|]\\s*)?`, "i");
  const endRe = new RegExp(`\\s*(?:[-–—:|]\\s*)?(?:${TOKS})\\b\\s*$`, "i");
  for (let i = 0; i < 3 && startRe.test(s); i++) s = s.replace(startRe, "").trim();
  for (let i = 0; i < 3 && endRe.test(s); i++) s = s.replace(endRe, "").trim();

  // Remove trailing trailer-related words (Official Trailer, Teaser Trailer, Trailer 2)
  s = s.replace(/\s*(?:[-–—:|]\s*)?(?:official\s+)?(?:teaser(?:\s+trailer)?|trailer(?:\s+\d+)?)\s*$/i, "");

  // If any token appears anywhere, cut the title at its first occurrence (keeps clean left part)
  const anyTok = new RegExp(`\\b(?:${TOKS})\\b`, "i");
  const cutPos = s.search(anyTok);
  if (cutPos > 0) s = s.slice(0, cutPos).trim();

  // Strip M3U/IPTV style attribute blobs like: tvg-logo="..." group-title="..."
  s = s.replace(/\s+[\w-]+\s*=\s*"[^"]*"/gi, "");
  s = s.replace(/\s+[\w-]+\s*=\s*'[^']*'/gi, "");

  // If attributes were present and there are commas, prefer the last comma segment as the title
  if (/(tvg-[\w-]+|group-title|https?:\/\/)/i.test(original) && s.includes(",")) {
    const last = s.split(",").pop()?.trim();
    if (last && last.length >= 2) s = last;
  }

  // Remove stray unmatched quotes
  s = s.replace(/["“”]+/g, "");

  // Remove trailing year, optionally wrapped or with separators, e.g., "Title - 2021", "Title (2021)"
  s = s.replace(/\s*(?:[-–—:|]\s*)?(?:\(|\[)?((?:19|20)\d{2})(?:\)|\])?\s*$/i, "");

  // Trim stray trailing separators and punctuation
  s = s.replace(/[\-–—:|,]+$/g, "").trim();

  // Collapse extra spaces
  s = s.replace(/\s{2,}/g, " ").trim();

  // Avoid empty titles
  if (!s) return original;
  return s;
}

function getYearFromTitle(t: string): number | undefined {
  const m = t.match(/\((19|20)\d{2}\)/);
  if (m) return parseInt(m[0].slice(1, -1), 10);
  return undefined;
}

function levenshtein(a: string, b: string): number {
  // Simple iterative Levenshtein distance; small inputs so OK for edge runtime
  const m = a.length, n = b.length;
  if (m === 0) return n;
  if (n === 0) return m;
  const dp = new Array(n + 1);
  for (let j = 0; j <= n; j++) dp[j] = j;
  for (let i = 1; i <= m; i++) {
    let prev = dp[0];
    dp[0] = i;
    for (let j = 1; j <= n; j++) {
      const temp = dp[j];
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      dp[j] = Math.min(
        dp[j] + 1,        // deletion
        dp[j - 1] + 1,    // insertion
        prev + cost       // substitution
      );
      prev = temp;
    }
  }
  return dp[n];
}

async function fetchTmdbDetailsById(id: number) {
  const url = `https://api.themoviedb.org/3/movie/${id}?api_key=${TMDB_API_KEY}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`TMDB details failed: ${res.status}`);
  return await res.json();
}

async function fetchTmdbAlternativeTitles(id: number) {
  const url = `https://api.themoviedb.org/3/movie/${id}/alternative_titles?api_key=${TMDB_API_KEY}`;
  const res = await fetch(url);
  if (!res.ok) return { titles: [] };
  const data = await res.json();
  const titles = Array.isArray(data?.titles) ? data.titles.map((t: any) => t.title).filter(Boolean) : [];
  return { titles } as { titles: string[] };
}

// Fetch TMDB videos for a movie (trailers, teasers, clips)
async function fetchTmdbVideosById(id: number) {
  const url = `https://api.themoviedb.org/3/movie/${id}/videos?api_key=${TMDB_API_KEY}`;
  const res = await fetch(url);
  if (!res.ok) return { results: [] };
  return await res.json();
}

// Choose the best trailer from TMDB videos
function pickBestTrailer(videos: any[]): { key: string; site: string; url: string } | null {
  if (!Array.isArray(videos) || videos.length === 0) return null;
  const byType = videos.filter((v) => /trailer/i.test(v?.type || ""));
  const pool = byType.length ? byType : videos; // fallback to any video
  // Prefer official, then site YouTube, then latest by published_at
  pool.sort((a: any, b: any) => {
    const offA = a?.official ? 1 : 0;
    const offB = b?.official ? 1 : 0;
    if (offA !== offB) return offB - offA;
    const siteRank = (s: string) => (/youtube/i.test(s) ? 2 : /vimeo/i.test(s) ? 1 : 0);
    const srA = siteRank(a?.site || "");
    const srB = siteRank(b?.site || "");
    if (srA !== srB) return srB - srA;
    const tA = Date.parse(a?.published_at || a?.publishedAt || 0) || 0;
    const tB = Date.parse(b?.published_at || b?.publishedAt || 0) || 0;
    return tB - tA;
  });
  const top = pool[0];
  const site = (top?.site || "").toString();
  const key = (top?.key || "").toString();
  if (!key) return null;
  let url = "";
  if (/youtube/i.test(site)) url = `https://www.youtube.com/watch?v=${key}`;
  else if (/vimeo/i.test(site)) url = `https://vimeo.com/${key}`;
  else url = key; // best effort
  return { key, site, url };
}

async function searchTmdbByTitle(title: string, year?: number, opts?: { language?: string; region?: string }) {
  const params: string[] = [
    `api_key=${TMDB_API_KEY}`,
    `query=${encodeURIComponent(title)}`,
    `page=1`,
    `include_adult=false`,
  ];
  if (year) params.push(`year=${year}`);
  if (opts?.language) params.push(`language=${encodeURIComponent(opts.language)}`);
  if (opts?.region) params.push(`region=${encodeURIComponent(opts.region)}`);
  const url = `https://api.themoviedb.org/3/search/movie?${params.join("&")}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`TMDB search failed: ${res.status}`);
  return await res.json();
}

type PickOpts = { year?: number; distanceThreshold?: number; altTitles?: string[] };
type PickResult = { candidate: any | null; confidence: number; reason: string };

function computeConfidence(qNorm: string, r: any, dist: number, year?: number, popMax?: number, popMin?: number): number {
  // Distance score [0..1]
  const maxLen = Math.max(qNorm.length, normalizeTitle(r.title || "").length, 1);
  const distScore = 1 - Math.min(1, dist / Math.max(1, Math.floor(maxLen * 0.6)));
  // Year score
  let yearScore = 0.5; // neutral if no info
  if (year) {
    const y = (r.release_date && typeof r.release_date === "string") ? parseInt(r.release_date.slice(0, 4), 10) : undefined;
    if (y) {
      const diff = Math.abs(y - year);
      yearScore = diff === 0 ? 1 : diff === 1 ? 0.75 : diff <= 2 ? 0.6 : 0.3;
    }
  }
  // Popularity score normalized across result set
  const pop = r.popularity ?? 0;
  const denom = Math.max(1, (popMax ?? pop) - (popMin ?? pop));
  const popScore = denom > 0 ? (pop - (popMin ?? pop)) / denom : 0.5;
  // Weighted blend
  return 0.55 * distScore + 0.25 * yearScore + 0.20 * popScore;
}

function pickBestMatchByTitle(query: string, results: any[], opts?: PickOpts): PickResult {
  if (!results?.length) return { candidate: null, confidence: 0, reason: "no_results" };
  const q = normalizeTitle(query);
  const threshold = opts?.distanceThreshold ?? Math.max(3, Math.floor(q.length * 0.25));

  // 1) Exact normalized title
  const exact = results.find((r) => normalizeTitle(r.title || "") === q);
  if (exact) return { candidate: exact, confidence: 1, reason: "exact_title_match" };

  // 2) Score candidates by normalized title distance and year proximity
  const pops = results.map((r) => r.popularity ?? 0);
  const popMax = Math.max(...pops);
  const popMin = Math.min(...pops);
  const scored = results.map((r) => {
    const rt = normalizeTitle(r.title || "");
    const dist = levenshtein(q, rt);
    let yearPenalty = 0;
    if (opts?.year) {
      const y = (r.release_date && typeof r.release_date === "string") ? parseInt(r.release_date.slice(0, 4), 10) : undefined;
      if (y && y !== opts.year) yearPenalty = 1; // small penalty if year mismatched
    }
    const confidence = computeConfidence(q, r, dist, opts?.year, popMax, popMin) - yearPenalty * 0.1;
    return { r, dist, yearPenalty, confidence };
  });

  const within = scored.filter((s) => s.dist <= threshold);
  const pickFrom = within.length ? within : scored;
  pickFrom.sort((a, b) => b.confidence - a.confidence);
  const top = pickFrom[0];
  if (!top) return { candidate: null, confidence: 0, reason: "no_results" };

  // 3) Alternative title exact check boosts confidence
  if (opts?.altTitles?.length) {
    const qset = new Set([q, normalizeTitle(stripParentheticals(query))]);
    const altHit = opts.altTitles.some((t) => qset.has(normalizeTitle(t)));
    if (altHit) {
      return { candidate: top.r, confidence: Math.max(0.9, top.confidence), reason: "alt_title_match" };
    }
  }

  return { candidate: top.r, confidence: top.confidence, reason: "best_scored" };
}

function tmdbToUpdatePatch(d: any) {
  return {
    tmdb_id: d?.id ?? null,
    overview: d?.overview ?? null,
    poster_path: d?.poster_path ?? null,
    backdrop_path: d?.backdrop_path ?? null,
    release_date: d?.release_date ?? null,
    vote_average: d?.vote_average ?? null,
    vote_count: d?.vote_count ?? null,
    popularity: d?.popularity ?? null,
    original_language: d?.original_language ?? null,
    original_title: d?.original_title ?? null,
    genre_ids: Array.isArray(d?.genres) ? d.genres.map((g: any) => g.id) : null,
    genres_json: Array.isArray(d?.genres) ? d.genres : null,
    runtime: d?.runtime ?? null,
    status: d?.status ?? null,
    tagline: d?.tagline ?? null,
    homepage: d?.homepage ?? null,
    imdb_id: d?.imdb_id ?? null,
    metadata_source: "tmdb",
    last_synced_at: new Date().toISOString(),
    title_normalized: d?.title ? normalizeTitle(d.title) : null,
  };
}

async function syncOne(movie: MovieRow, force = false, options?: { tryCount?: number; language?: string; region?: string }) {
  try {
    let details: any | null = null;

    if (movie.tmdb_id && !force) {
      details = await fetchTmdbDetailsById(movie.tmdb_id);
    } else {
      // Staged strategies based on tryCount (default 0)
      const tryCount = Math.max(0, options?.tryCount ?? 0);
      const title = movie.title || "";
      const yearGuess = getYearFromTitle(title);
      const stripped = stripParentheticals(title).replace(/\s*[:\-–].*$/, "").trim();

      // Strategy order across retries
      const strategies: Array<{ key: string; title: string; year?: number; distanceThreshold?: number }> = [];
      strategies.push({ key: "orig+year", title, year: yearGuess, distanceThreshold: undefined }); // t0: original + year
      strategies.push({ key: "orig", title, year: undefined, distanceThreshold: undefined });      // t1: original no year
      strategies.push({ key: "stripped+year", title: stripped, year: yearGuess, distanceThreshold: undefined }); // t2
      strategies.push({ key: "stripped", title: stripped, year: undefined, distanceThreshold: undefined });       // t3
      strategies.push({ key: "stripped+year+loose", title: stripped, year: yearGuess, distanceThreshold: 5 });    // t4+

      const idx = Math.min(tryCount, strategies.length - 1);
      const strat = strategies[idx];

      const search = await searchTmdbByTitle(strat.title, strat.year, { language: options?.language, region: options?.region });
      // Try to leverage alternative titles for the top candidate validation
      let altTitles: string[] = [];
      try {
        const topId = search?.results?.[0]?.id;
        if (topId) {
          const alt = await fetchTmdbAlternativeTitles(topId);
          altTitles = alt.titles || [];
        }
      } catch {}

      const picked = pickBestMatchByTitle(strat.title, search?.results || [], { year: strat.year, distanceThreshold: strat.distanceThreshold, altTitles });
      const CONF_THRESHOLD = 0.62; // accept only confident matches
      if (!picked?.candidate || picked.confidence < CONF_THRESHOLD) {
        return { ok: false, reason: "no_tmdb_match" as const };
      }
      details = await fetchTmdbDetailsById(picked.candidate.id);
    }
    // Fetch trailer info
    let trailer: { key: string; site: string; url: string } | null = null;
    try {
      const vids = await fetchTmdbVideosById(details.id);
      trailer = pickBestTrailer(vids?.results || []);
    } catch {}

    const patch = tmdbToUpdatePatch(details);
    if (trailer) {
      (patch as any).trailer_key = trailer.key;
      (patch as any).trailer_site = trailer.site;
      (patch as any).trailer_url = trailer.url;
    }
    const { error } = await supabase.from("movies").update(patch).eq("id", movie.id);
    if (error) throw error;

    return { ok: true as const, tmdb_id: patch.tmdb_id };
  } catch (e: any) {
    return { ok: false as const, reason: e?.message ?? "unknown_error" };
  }
}

// =================== Maintenance: requeue, repair titles, deduplicate ===================

async function maybeEnqueue(movieId: number, reason: string): Promise<boolean> {
  // Avoid duplicate queue entries; if an entry exists and is scheduled, skip.
  const { data: existing, error: fetchErr } = await supabase
    .from("movies_sync_queue")
    .select("id")
    .eq("movie_id", movieId)
    .maybeSingle();
  if (existing?.id) {
    const soon = new Date();
    soon.setMinutes(soon.getMinutes() + 1);
    const { error } = await supabase
      .from("movies_sync_queue")
      .update({ reason, next_try_at: soon.toISOString() })
      .eq("id", existing.id);
    if (error) console.warn("queue update failed", { movieId, error: error.message });
    return false;
  } else {
    const { error } = await supabase
      .from("movies_sync_queue")
      .insert({ movie_id: movieId, try_count: 0, next_try_at: new Date().toISOString(), reason });
    if (error) {
      console.warn("queue insert failed", { movieId, error: error.message });
      return false;
    }
    return true;
  }
}

async function scanAndRequeueMissing(limit = 500): Promise<{ scanned: number; enqueued: number }> {
  // Requeue when any metadata field present on rows is missing
  // We avoid information_schema and simply select all columns, then detect missing fields client-side.
  const { data, error } = await supabase
    .from("movies")
    .select("*")
    .order("id", { ascending: true })
    .limit(limit);
  if (error) throw error;

  const isEmptyText = (v: any) => v === null || v === undefined || (typeof v === "string" && v.trim() === "");
  const isEmptyArr = (v: any) => !Array.isArray(v) || v.length === 0;

  let enq = 0;
  for (const row of data ?? []) {
    const r = row as any;
    const missing: string[] = [];
    const checkIfPresent = (key: string, pred: (v: any) => boolean) => {
      if (key in r && pred((r as any)[key])) missing.push(key);
    };
    checkIfPresent("tmdb_id", (v) => !v);
    checkIfPresent("overview", isEmptyText);
    checkIfPresent("poster_path", isEmptyText);
    checkIfPresent("backdrop_path", isEmptyText);
    checkIfPresent("release_date", isEmptyText);
    checkIfPresent("vote_average", (v) => v == null);
    checkIfPresent("vote_count", (v) => v == null);
    checkIfPresent("popularity", (v) => v == null);
    checkIfPresent("original_language", isEmptyText);
    checkIfPresent("original_title", isEmptyText);
    // If both genre fields exist but both empty/absent, count as missing
    const hasGenreIds = "genre_ids" in r;
    const hasGenresJson = "genres_json" in r;
    if ((hasGenreIds || hasGenresJson) && (isEmptyArr(r.genre_ids) && isEmptyArr(r.genres_json))) missing.push("genres");
    checkIfPresent("runtime", (v) => v == null);
    checkIfPresent("status", isEmptyText);
    checkIfPresent("homepage", isEmptyText);
    checkIfPresent("imdb_id", isEmptyText);
    checkIfPresent("tagline", isEmptyText);
    checkIfPresent("trailer_url", isEmptyText);
    checkIfPresent("trailer_key", isEmptyText);
    checkIfPresent("trailer_site", isEmptyText);
    checkIfPresent("title_normalized", isEmptyText);

    if (missing.length) {
      const reason = missing.includes("tmdb_id")
        ? "missing_tmdb"
        : `missing_metadata:${missing.slice(0, 4).join("|")}`; // include a few keys for debugging
      const ok = await maybeEnqueue(r.id, reason);
      if (ok) enq += 1;
    }
  }
  return { scanned: (data ?? []).length, enqueued: enq };
}

async function repairTitles(limit = 200): Promise<{ checked: number; updated: number }> {
  // Movies with tmdb_id present but titles likely off vs TMDB
  const { data, error } = await supabase
    .from("movies")
    .select("id,title,tmdb_id")
    .not("tmdb_id", "is", null)
    .limit(limit);
  if (error) throw error;
  let updated = 0;
  for (const row of (data ?? [])) {
    const m = row as any;
    try {
      const det = await fetchTmdbDetailsById(m.tmdb_id);
      const local = normalizeTitle(m.title || "");
      const remote = normalizeTitle(det?.title || "");
      if (!remote) continue;
      const dist = levenshtein(local, remote);
      const maxLen = Math.max(local.length, remote.length, 1);
      const ratio = dist / Math.max(1, Math.floor(maxLen * 0.6));
      if (ratio > 0.4) {
        // Significantly different; prefer TMDB official title
        const patch = {
          title: det.title,
          title_normalized: normalizeTitle(det.title || ""),
        } as any;
        const { error: uerr } = await supabase.from("movies").update(patch).eq("id", m.id);
        if (!uerr) updated += 1;
      }
    } catch (e: any) {
      console.warn("repairTitles failed", { id: m.id, err: e?.message });
    }
  }
  return { checked: (data ?? []).length, updated };
}

// Format/sanitize local titles regardless of TMDB, e.g., remove prefixes and trailing years
async function sanitizeTitles(limit = 500): Promise<{ checked: number; updated: number }> {
  const { data, error } = await supabase
    .from("movies")
    .select("id,title")
    .limit(limit);
  if (error) throw error;
  let updated = 0;
  for (const row of (data ?? [])) {
    const m = row as any;
    try {
      const cleaned = cleanTitleForDisplay(m.title || "");
      if (cleaned && cleaned !== m.title) {
        const patch = {
          title: cleaned,
          title_normalized: normalizeTitle(cleaned),
        } as any;
        const { error: uerr } = await supabase.from("movies").update(patch).eq("id", m.id);
        if (!uerr) updated += 1;
      }
    } catch (e: any) {
      console.warn("sanitizeTitles failed", { id: m.id, err: e?.message });
    }
  }
  return { checked: (data ?? []).length, updated };
}

type DupGroup = { tmdb_id: number; ids: string[] };

async function findDuplicatesByTmdbId(limit = 2000): Promise<DupGroup[]> {
  // Fetch a reasonable window and group in memory
  const { data, error } = await supabase
    .from("movies")
    .select("id,tmdb_id,overview,poster_path,vote_count")
    .not("tmdb_id", "is", null)
    .limit(limit);
  if (error) throw error;
  const groups = new Map<number, string[]>();
  for (const row of data ?? []) {
    const r = row as any;
    const key = r.tmdb_id as number;
    const arr = groups.get(key) ?? [];
    arr.push(r.id);
    groups.set(key, arr);
  }
  const dups: DupGroup[] = [];
  for (const [k, arr] of groups.entries()) {
    if (arr.length > 1) dups.push({ tmdb_id: k, ids: arr });
  }
  return dups;
}

async function deduplicateByTmdbId(dryRun = true, limitScan = 2000): Promise<{ groups: number; deleted: number; kept: number }> {
  const groups = await findDuplicatesByTmdbId(limitScan);
  let deleted = 0;
  let kept = 0;
  for (const g of groups) {
    // Heuristic: keep the id that has most completeness (poster+overview) and highest vote_count
    const { data, error } = await supabase
      .from("movies")
      .select("id,overview,poster_path,vote_count")
      .in("id", g.ids);
    if (error) continue;
    const scored = (data ?? []).map((r: any) => ({
      id: r.id,
      score: (r.poster_path ? 1 : 0) + (r.overview ? 1 : 0) + (r.vote_count ?? 0) * 0.001,
    }));
    scored.sort((a, b) => b.score - a.score);
    const keepId = scored[0]?.id ?? g.ids[0];
    const drop = g.ids.filter((id) => id !== keepId);
    kept += 1;
    if (!dryRun && drop.length) {
      const { error: derr } = await supabase.from("movies").delete().in("id", drop);
      if (!derr) deleted += drop.length;
    }
  }
  return { groups: groups.length, deleted, kept };
}

async function handleMaintenance(req: Request) {
  try {
    const body = (await req.json().catch(() => ({}))) as any;
    const limit = Math.min(Math.max(body?.limit ?? 500, 1), 5000);
    const repairLimit = Math.min(Math.max(body?.repairLimit ?? 200, 1), 2000);
    const formatLimit = Math.min(Math.max(body?.formatLimit ?? repairLimit, 1), 5000);
    const dupScanLimit = Math.min(Math.max(body?.duplicateScanLimit ?? 2000, 100), 10000);
    const dryRunDedup = body?.dryRunDedup ?? true;
    const formatOnly = body?.formatOnly ?? false;

    if (formatOnly) {
      const format = await sanitizeTitles(formatLimit);
      return new Response(JSON.stringify({ format }), { status: 200 });
    }

    const requeue = await scanAndRequeueMissing(limit);
    const repair = await repairTitles(repairLimit);
    const format = await sanitizeTitles(formatLimit);
    const dedup = await deduplicateByTmdbId(dryRunDedup, dupScanLimit);

    return new Response(
      JSON.stringify({ requeue, repair, format, dedup, dryRunDedup }),
      { status: 200 }
    );
  } catch (e: any) {
    return new Response(JSON.stringify({ error: e?.message ?? "maintenance_failed" }), { status: 500 });
  }
}

// =================== Queue worker for continuous syncing ===================

type QueueRow = {
  id: string;
  movie_id: string;
  tmdb_id: number | null;
  try_count: number;
  next_try_at: string;
  reason?: string | null;
};

function computeBackoff(nextTryCount: number, reason?: string): string {
  // Exponential backoff in minutes
  let minutes = Math.min(60, Math.pow(2, Math.max(0, nextTryCount)));
  // For no_tmdb_match, back off more aggressively (up to 12 hours)
  if (reason === "no_tmdb_match") {
    minutes = Math.min(720, minutes * 6);
  }
  const d = new Date();
  d.setMinutes(d.getMinutes() + minutes);
  return d.toISOString();
}

async function fetchPendingJobs(limit = 50): Promise<QueueRow[]> {
  const nowIso = new Date().toISOString();
  const { data, error } = await supabase
    .from("movies_sync_queue")
    .select("id,movie_id,tmdb_id,try_count,next_try_at,reason")
    .lte("next_try_at", nowIso)
    .order("created_at", { ascending: true })
    .limit(limit);
  if (error) throw error;
  return (data as QueueRow[]) ?? [];
}

async function loadMovieRow(movieId: string): Promise<MovieRow | null> {
  const { data, error } = await supabase
    .from("movies")
    .select("id,title,tmdb_id")
    .eq("id", movieId)
    .maybeSingle();
  if (error) throw error;
  return data as MovieRow | null;
}

async function completeJob(jobId: string): Promise<void> {
  const { error } = await supabase.from("movies_sync_queue").delete().eq("id", jobId);
  if (error) throw error;
}

async function rescheduleJob(job: QueueRow, reason?: string): Promise<void> {
  const nextTryAt = computeBackoff(job.try_count + 1, reason);
  const { error } = await supabase
    .from("movies_sync_queue")
    .update({ try_count: job.try_count + 1, next_try_at: nextTryAt, reason })
    .eq("id", job.id);
  if (error) throw error;
}

const MAX_TRIES = 8;

async function processQueueBatch(maxBatch = 50) {
  const jobs = await fetchPendingJobs(maxBatch);
  if (!jobs.length) return { total: 0, synced: 0 };

  let done = 0;
  for (const job of jobs) {
    try {
      const movie = await loadMovieRow(job.movie_id);
      if (!movie) {
        // Movie no longer exists; drop the job
        await completeJob(job.id);
        continue;
      }
      const result = await syncOne(movie, false, { tryCount: job.try_count });
      if (result.ok) {
        await completeJob(job.id);
        done += 1;
      } else {
        const reason = (result as any).reason as string | undefined;
        const nextTryCount = job.try_count + 1;
        if (reason === "no_tmdb_match" && nextTryCount >= MAX_TRIES) {
          // Promote to manual mapping after cap
          console.warn("Cap reached; marking manual mapping:", { jobId: job.id, movieId: job.movie_id });
          // Complete job to avoid infinite retries
          await completeJob(job.id);
        } else {
          await rescheduleJob(job, reason);
        }
      }
    } catch (e: any) {
      await rescheduleJob(job, e?.message ?? "worker_error");
    }
  }
  return { total: jobs.length, synced: done };
}

async function handleWorker() {
  try {
    const summary = await processQueueBatch(50);
    return new Response(JSON.stringify(summary), { status: 200 });
  } catch (e: any) {
    return new Response(JSON.stringify({ error: e?.message ?? "worker_failed" }), { status: 500 });
  }
}

async function handleSingleSync(req: Request) {
  const raw = await req.json().catch(() => ({} as any));
  const record = raw && typeof raw === "object" && "record" in raw ? (raw as any).record : raw;
  const payload: SyncPayload = {
    id: (record?.id ?? raw?.id) as string | undefined,
    title: (record?.title ?? raw?.title) as string | undefined,
    force: (raw?.force ?? false) as boolean,
  };
  if (!payload?.id && !payload?.title) {
    return new Response(JSON.stringify({ error: "id or title required" }), { status: 400 });
  }

  let query = supabase.from("movies").select("id,title,tmdb_id").limit(1);
  if (payload.id) query = query.eq("id", payload.id);
  else if (payload.title) query = query.eq("title", payload.title);

  const { data, error } = await query.maybeSingle();
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  if (!data) return new Response(JSON.stringify({ error: "movie_not_found" }), { status: 404 });

  const result = await syncOne(data as MovieRow, !!payload.force);
  const code = result.ok ? 200 : 422;
  return new Response(JSON.stringify(result), { status: code });
}

async function handleBulkSync(req: Request) {
  const payload = (await req.json().catch(() => ({}))) as BulkPayload;
  const limit = Math.min(Math.max(payload.limit ?? 200, 1), 2000);
  const missingOnly = payload.missingOnly ?? true;

  let query = supabase.from("movies").select("id,title,tmdb_id").limit(limit);
  if (missingOnly) query = query.is("tmdb_id", null);

  const { data, error } = await query;
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });

  const jobs = (data ?? []).map((row) => syncOne(row as MovieRow, false));
  const results = await Promise.all(jobs);
  const okCount = results.filter((r) => r.ok).length;

  return new Response(JSON.stringify({ total: results.length, synced: okCount }), { status: 200 });
}

Deno.serve(async (req) => {
  const hdr = req.headers.get("x-admin-token") ?? "";
  const okAuth = ADMIN_TOKEN && hdr && hdr === ADMIN_TOKEN;
  if (!okAuth) {
    // Minimal diagnostics without leaking secrets
    console.warn(
      "Unauthorized request:",
      JSON.stringify({ hdrLen: hdr?.length ?? 0, envLen: ADMIN_TOKEN?.length ?? 0 })
    );
    return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401 });
  }

  const url = new URL(req.url);
  // Allow a worker mode via GET (for scheduled runs) or POST to /worker
  if ((req.method === "GET" && (url.searchParams.get("mode") === "worker")) ||
      (req.method === "POST" && url.pathname.endsWith("/worker"))) {
    return await handleWorker();
  }
  // Maintenance mode via GET mode=maintenance or POST /maintenance
  if ((req.method === "GET" && (url.searchParams.get("mode") === "maintenance")) ||
      (req.method === "POST" && url.pathname.endsWith("/maintenance"))) {
    return await handleMaintenance(req);
  }
  if (req.method === "POST" && url.pathname.endsWith("/bulk")) {
    return await handleBulkSync(req);
  }
  if (req.method === "POST") {
    return await handleSingleSync(req);
  }
  return new Response("OK", { status: 200 });
});
