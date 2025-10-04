/// <reference lib="dom" />
// Deno Deploy Edge Function: video_hosting_worker
// Purpose: Direct-link already uploaded stream URLs to existing movies without re-uploading.
// Notes: Provides preview and commit endpoints for safe mapping.
// Routes (Direct-Link only):
//   POST /link/preview      -> preview candidate movies for a given stream URL
//   POST /link/commit       -> commit URL to a movie (with safety checks)
//   POST /link/bulk-commit  -> commit multiple URL->movie mappings
//   POST /link/auto         -> auto-match and commit URL to best movie candidate
// Auth: x-admin-token header must equal ADMIN_TOKEN

// @ts-ignore
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
// @ts-ignore
declare const Deno: any;

const SUPABASE_URL = Deno.env.get("SUPABASE_URL");
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
const ADMIN_TOKEN = Deno.env.get("ADMIN_TOKEN");
const TARGET_HOST = "r2";
const R2_PUBLIC_BASE_URL = (Deno.env.get("R2_PUBLIC_BASE_URL") || "").replace(/\/$/, "");

if (!SUPABASE_URL || !SERVICE_ROLE || !ADMIN_TOKEN) {
  console.error("Missing required env for video_hosting_worker.");
}

const supabase = createClient(SUPABASE_URL!, SERVICE_ROLE!);

// (Legacy job types removed)

function needsHosting(url: string | null | undefined, host: string | null | undefined): boolean {
  if (!url) return false; // nothing to host
  if (host !== TARGET_HOST) return true;
  if (R2_PUBLIC_BASE_URL && !url.startsWith(R2_PUBLIC_BASE_URL)) return true;
  return false;
}

// (Legacy backoff removed)

// (Legacy enqueue scan removed)

// (Legacy leasing removed)

// (Legacy complete removed)

// (Legacy reschedule removed)

// (Legacy maintenance removed)

// (Legacy lease route removed)

// (Legacy complete route removed)

// (Legacy reschedule route removed)

// (Legacy enqueue route removed)

// (Legacy resolver removed)

// (Legacy resolver strategies removed)

// (Legacy UA/referrer helpers removed)

// (Legacy clear route removed)

// (Legacy bulk enqueue removed)

// ===== Direct Link Workflow (no re-upload) =====
// Preview match candidates for a raw stream URL and optionally commit an update to movies.videourl
// This is designed to safely bypass download/upload by linking existing streams to the right movie.

type LinkPreviewBody = {
  url: string;
  headers?: Record<string, string> | null;
  titleHint?: string | null;
  yearHint?: number | null;
  movieId?: string | null; // if provided, we only validate and score that movie
};

type LinkCommitBody = {
  movieId: string;
  url: string;
  headers?: Record<string, string> | null;
  force?: boolean; // if true, allow replacing an existing videourl
};

type LinkAutoBody = {
  url: string;
  headers?: Record<string, string> | null;
  minScore?: number; // minimum confidence score (0-1), default 0.6
  force?: boolean; // if true, allow replacing an existing videourl
};

type RegisterSignatureBody = {
  movieId: string;
  url: string;
  headers?: Record<string, string> | null;
};

type RegisterProviderMapBody = {
  movieId: string;
  url?: string; // if provided, extract provider + id from URL
  providerHost?: string; // alternative direct input
  providerId?: string;   // alternative direct input
};

function normalizeForMatch(raw: string): string {
  try {
    const u = new URL(raw);
    // strip common volatile query params
    const paramsToDrop = new Set(["token", "signature", "expires", "expiry", "X-Amz-Signature", "X-Amz-Date", "Policy", "Key-Pair-Id"]);
    const keep = new URLSearchParams();
    u.searchParams.forEach((v, k) => {
      if (!paramsToDrop.has(k)) keep.append(k, v);
    });
    const cleaned = `${u.protocol}//${u.host}${u.pathname}${keep.toString() ? "?" + keep.toString() : ""}`;
    return cleaned.replace(/\/+$/g, "");
  } catch {
    return raw;
  }
}

function extractFilenameTokens(urlOrName: string): { tokens: string[]; year?: number } {
  const decoded = decodeURIComponent(urlOrName);
  const base = decoded.split("/").pop() || decoded;
  const name = base.replace(/\.(mp4|mkv|avi|mov|m3u8|ts)$/i, "");
  // Remove common release tags
  const cleaned = name
    .replace(/\b(1080p|720p|480p|2160p|4k|x264|x265|hevc|bluray|webrip|web[-_. ]dl|hdr|h265|dv|remux|cam|hdts|hdtc|proper|repack)\b/gi, " ")
    .replace(/[._-]+/g, " ")
    .toLowerCase();
  const yearMatch = cleaned.match(/\b(19\d{2}|20\d{2})\b/);
  const year = yearMatch ? Number(yearMatch[1]) : undefined;
  const tokens = cleaned
    .split(/\s+/)
    .filter(Boolean)
    .filter((t) => !/^s\d{1,2}e\d{1,2}$/i.test(t)) // remove sxxexx
    .filter((t) => t.length >= 2);
  return { tokens, year };
}

function jaccard(a: Set<string>, b: Set<string>): number {
  const inter = new Set([...a].filter((x) => b.has(x)));
  const union = new Set([...a, ...b]);
  return union.size === 0 ? 0 : inter.size / union.size;
}

function tokenizeTitle(raw: unknown): Set<string> {
  const s = String(raw ?? "").toLowerCase();
  const arr = s.split(/\s+/).filter(Boolean);
  return new Set<string>(arr as string[]);
}

function guessHostForUrl(u: string): string {
  if (R2_PUBLIC_BASE_URL && u.startsWith(R2_PUBLIC_BASE_URL)) return TARGET_HOST;
  return "external"; // not hosted on R2
}

async function fetchMoviesForMatching(tokens: string[], yearHint?: number | null) {
  // Narrow search using ILIKE on first few tokens if title column exists
  const narrowTokens = tokens.slice(0, 3);
  let candidates: any[] = [];

  // Attempt 1: title + optional year
  const ors = narrowTokens.map((t) => `title.ilike.%${t}%`).join(",");
  let query = supabase
    .from("movies")
    .select("id,title,release_date,videourl,video_host");
  if (ors.length > 0) {
    query = query.or(ors);
  }
  if (yearHint) {
    // If we have a year hint, filter release_date by year range when available
    const start = `${yearHint}-01-01`;
    const end = `${yearHint + 1}-01-01`;
    query = query.gte("release_date", start).lt("release_date", end);
  }
  let { data, error } = await query.limit(200);
  if (!error && Array.isArray(data)) {
    candidates = data as any[];
  } else {
    // Fallback: minimal select if columns differ
    const fb = await supabase.from("movies").select("id,videourl,video_host").limit(200);
    candidates = (fb.data as any[]) ?? [];
  }
  return candidates;
}

// Attempt to extract a stable provider identifier from the URL
// Example matches:
//  - UUID-like: D8E41AD0-E273-D7D4-A92A-2579BCC6C8A9
//  - Long alphanumeric tokens: >=16 chars
function extractProviderIdFromUrl(raw: string): { providerHost: string; providerId: string } | null {
  try {
    const u = new URL(raw);
    const host = u.host.toLowerCase();
    const path = decodeURIComponent(u.pathname);
    const uuidLike = path.match(/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/);
    if (uuidLike) return { providerHost: host, providerId: uuidLike[0] };
    const longToken = path.match(/([A-Za-z0-9]{16,})/);
    if (longToken) return { providerHost: host, providerId: longToken[1] };
    return null;
  } catch {
    return null;
  }
}

// Fetch helpers for signature computation
async function fetchHead(url: string, headers?: Record<string, string> | null): Promise<{ size: number | null; acceptRanges: boolean; contentType: string | null }> {
  try {
    const res = await fetch(url, { method: "HEAD", headers: headers ?? undefined });
    const len = res.headers.get("content-length");
    const acceptRanges = (res.headers.get("accept-ranges") || "").toLowerCase().includes("bytes");
    const ct = res.headers.get("content-type");
    return { size: len ? Number(len) : null, acceptRanges, contentType: ct };
  } catch {
    return { size: null, acceptRanges: false, contentType: null };
  }
}

async function fetchRange(url: string, start: number, endInclusive: number, headers?: Record<string, string> | null): Promise<ArrayBuffer | null> {
  try {
    const hdrs = new Headers(headers ?? undefined);
    hdrs.set("Range", `bytes=${start}-${endInclusive}`);
    const res = await fetch(url, { method: "GET", headers: hdrs });
    if (!res.ok) return null;
    const ab = await res.arrayBuffer();
    return ab;
  } catch {
    return null;
  }
}

async function sha256Hex(buf: ArrayBuffer): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", buf);
  const bytes = new Uint8Array(digest);
  return Array.from(bytes).map(b => b.toString(16).padStart(2, "0")).join("");
}

// Compute a lightweight signature for a media file using head/tail chunks
async function computeFileSignature(url: string, headers?: Record<string, string> | null): Promise<{ sizeBytes: number | null; headSha256: string | null; tailSha256: string | null }> {
  const headInfo = await fetchHead(url, headers);
  const size = headInfo.size;
  const CHUNK = 1 * 1024 * 1024; // 1 MiB
  // Fallbacks when HEAD doesn't work or size is unknown
  if (!size || size <= 0) {
    const first = await fetchRange(url, 0, CHUNK - 1, headers);
    if (!first) return { sizeBytes: null, headSha256: null, tailSha256: null };
    const firstHash = await sha256Hex(first);
    return { sizeBytes: null, headSha256: firstHash, tailSha256: null };
  }
  const headChunk = await fetchRange(url, 0, Math.min(CHUNK, size) - 1, headers);
  const tailStart = size > CHUNK ? size - CHUNK : 0;
  const tailChunk = await fetchRange(url, tailStart, size - 1, headers);
  const headHash = headChunk ? await sha256Hex(headChunk) : null;
  const tailHash = tailChunk ? await sha256Hex(tailChunk) : null;
  return { sizeBytes: size, headSha256: headHash, tailSha256: tailHash };
}

async function handleLinkPreview(req: Request) {
  const body = (await req.json().catch(() => ({}))) as LinkPreviewBody;
  if (!body.url) return new Response(JSON.stringify({ error: "missing_url" }), { status: 400 });

  const normalizedUrl = normalizeForMatch(body.url);
  const { tokens, year } = extractFilenameTokens(normalizedUrl);
  const yearHint = body.yearHint ?? year ?? null;

  // 1) Provider ID fast-path
  try {
    const pid = extractProviderIdFromUrl(body.url);
    if (pid) {
      const { data: mapRow, error: mapErr } = await supabase
        .from("external_stream_map")
        .select("movie_id")
        .eq("provider_host", pid.providerHost)
        .eq("provider_id", pid.providerId)
        .maybeSingle();
      if (!mapErr && mapRow?.movie_id) {
        const { data: mr, error: mErr } = await supabase
          .from("movies")
          .select("id,title,release_date,videourl,video_host")
          .eq("id", mapRow.movie_id)
          .maybeSingle();
        if (!mErr && mr) {
          return new Response(
            JSON.stringify({
              normalizedUrl,
              candidates: [
                {
                  movie: mr,
                  score: 1,
                  reasons: ["provider_map_match"],
                },
              ],
            }),
            { status: 200 },
          );
        }
      }
    }
  } catch {
    // ignore provider-map errors
  }

  // 2) File signature match
  try {
    const sig = await computeFileSignature(body.url, body.headers ?? null);
    if (sig.headSha256 || (sig.sizeBytes && sig.tailSha256)) {
      let query = supabase
        .from("movie_file_signatures")
        .select("movie_id,size_bytes,head_sha256,tail_sha256")
        .limit(1);
      if (sig.sizeBytes) query = query.eq("size_bytes", sig.sizeBytes);
      if (sig.headSha256) query = query.eq("head_sha256", sig.headSha256);
      if (sig.tailSha256) query = query.eq("tail_sha256", sig.tailSha256);
      const { data: sigRow, error: sErr } = await query.maybeSingle();
      if (!sErr && sigRow?.movie_id) {
        const { data: mr, error: mErr } = await supabase
          .from("movies")
          .select("id,title,release_date,videourl,video_host")
          .eq("id", sigRow.movie_id)
          .maybeSingle();
        if (!mErr && mr) {
          return new Response(
            JSON.stringify({
              normalizedUrl,
              candidates: [
                {
                  movie: mr,
                  score: 1,
                  reasons: [
                    sig.sizeBytes ? `signature_size:${sig.sizeBytes}` : undefined,
                    sig.headSha256 ? "signature_head" : undefined,
                    sig.tailSha256 ? "signature_tail" : undefined,
                  ].filter(Boolean),
                },
              ],
            }),
            { status: 200 },
          );
        }
      }
    }
  } catch {
    // ignore signature errors
  }

  // If a movieId is provided, just validate and return that as the only candidate with a score
  if (body.movieId) {
    const { data: mr, error } = await supabase
      .from("movies")
      .select("id,title,release_date,videourl,video_host")
      .eq("id", body.movieId)
      .maybeSingle();
    if (error || !mr) return new Response(JSON.stringify({ error: "movie_not_found" }), { status: 404 });
    const titleTokens = tokenizeTitle(mr.title);
    const urlSet = new Set(tokens);
    const baseScore = jaccard(titleTokens, urlSet);
    const score = Math.min(1, baseScore + (yearHint && mr.release_date?.startsWith(String(yearHint)) ? 0.15 : 0));
    return new Response(
      JSON.stringify({
        normalizedUrl,
        candidates: [
          {
            movie: mr,
            score,
            reasons: [
              `title_overlap:${baseScore.toFixed(2)}`,
              yearHint ? `year_hint:${yearHint}` : undefined,
            ].filter(Boolean),
          },
        ],
      }),
      { status: 200 },
    );
  }

  const candidates = await fetchMoviesForMatching(tokens, yearHint);
  const urlTokens = new Set(tokens);
  const ranked = candidates
    .map((m) => {
      const titleTokens = tokenizeTitle(m.title);
      const base = jaccard(titleTokens, urlTokens);
      const yearBoost = yearHint && m.release_date?.startsWith(String(yearHint)) ? 0.15 : 0;
      const penaltyAlreadyHosted = needsHosting(m.videourl, m.video_host) ? 0 : -0.1; // de-prioritize already hosted rows
      const score = Math.max(0, Math.min(1, base + yearBoost + penaltyAlreadyHosted));
      return {
        movie: m,
        score,
        reasons: [
          `title_overlap:${base.toFixed(2)}`,
          yearHint ? `year_hint:${yearHint}` : undefined,
          !needsHosting(m.videourl, m.video_host) ? "already_hosted" : undefined,
        ].filter(Boolean),
      };
    })
    .sort((a, b) => b.score - a.score)
    .slice(0, 20);

  return new Response(
    JSON.stringify({ normalizedUrl, candidates: ranked }),
    { status: 200 },
  );
}

async function handleLinkCommit(req: Request) {
  const body = (await req.json().catch(() => ({}))) as LinkCommitBody;
  const movieId = body.movieId;
  const newUrl = body.url;
  if (!movieId || !newUrl) return new Response(JSON.stringify({ error: "missing_params" }), { status: 400 });

  // Safety: prevent accidental duplicates
  const { data: dup } = await supabase
    .from("movies")
    .select("id")
    .eq("videourl", newUrl)
    .neq("id", movieId)
    .maybeSingle();
  if (dup?.id) return new Response(JSON.stringify({ error: "url_already_linked_to_other_movie", otherMovieId: dup.id }), { status: 409 });

  const { data: mrow } = await supabase
    .from("movies")
    .select("id,videourl,original_videourl")
    .eq("id", movieId)
    .maybeSingle();
  if (!mrow) return new Response(JSON.stringify({ error: "movie_not_found" }), { status: 404 });

  if (mrow.videourl && !body.force) {
    return new Response(JSON.stringify({ error: "movie_already_has_videourl", hint: "pass force=true to override" }), { status: 409 });
  }

  const host = guessHostForUrl(newUrl);
  const patch: any = {
    videourl: newUrl,
    video_host: host,
    video_public_id: null,
    video_uploaded_at: new Date().toISOString(),
  };
  if (!mrow.original_videourl) patch.original_videourl = mrow.videourl ?? null;

  const { error: uerr } = await supabase.from("movies").update(patch).eq("id", movieId);
  if (uerr) return new Response(JSON.stringify({ error: uerr.message }), { status: 500 });

  // Clean up any pending queue for this movie
  try {
    await supabase.from("videos_sync_queue").delete().eq("movie_id", movieId);
  } catch {
    // ignore if table does not exist or other non-critical errors
  }

  return new Response(JSON.stringify({ ok: true, movieId, host }), { status: 200 });
}

async function handleLinkBulkCommit(req: Request) {
  const body = (await req.json().catch(() => ({}))) as { items: Array<LinkCommitBody> };
  const items = body.items ?? [];
  if (!Array.isArray(items) || items.length === 0) return new Response(JSON.stringify({ error: "empty_items" }), { status: 400 });
  const results: any[] = [];
  for (const item of items) {
    try {
      const r = await handleLinkCommit(new Request("", { method: "POST", body: JSON.stringify(item) }));
      const payload = await r.json();
      results.push({ movieId: item.movieId, status: r.status, ...payload });
    } catch (e: any) {
      results.push({ movieId: item.movieId, status: 500, error: String(e?.message || e) });
    }
  }
  return new Response(JSON.stringify({ ok: true, results }), { status: 200 });
}

async function handleLinkAuto(req: Request) {
  const body = (await req.json().catch(() => ({}))) as LinkAutoBody;
  const url = body.url;
  if (!url) return new Response(JSON.stringify({ error: "missing_url" }), { status: 400 });

  const minScore = body.minScore ?? 0.6;
  const normalizedUrl = normalizeForMatch(url);
  const { tokens, year } = extractFilenameTokens(normalizedUrl);

  // 0) Safety: prevent accidental duplicates (keep as-is)

  // Safety: prevent accidental duplicates
  const { data: dup } = await supabase
    .from("movies")
    .select("id,title")
    .eq("videourl", url)
    .maybeSingle();
  if (dup?.id) return new Response(JSON.stringify({ error: "url_already_linked", movieId: dup.id, title: dup.title }), { status: 409 });

  // 1) Provider map auto-match
  try {
    const pid = extractProviderIdFromUrl(url);
    if (pid) {
      const { data: mapRow, error: mapErr } = await supabase
        .from("external_stream_map")
        .select("movie_id")
        .eq("provider_host", pid.providerHost)
        .eq("provider_id", pid.providerId)
        .maybeSingle();
      if (!mapErr && mapRow?.movie_id) {
        const { data: mrow } = await supabase
          .from("movies")
          .select("id,title,videourl")
          .eq("id", mapRow.movie_id)
          .maybeSingle();
        if (mrow) {
          if (mrow.videourl && !body.force) {
            return new Response(JSON.stringify({ 
              error: "movie_already_has_videourl", 
              movieId: mrow.id, 
              title: mrow.title,
              existingUrl: mrow.videourl,
              hint: "pass force=true to override" 
            }), { status: 409 });
          }
          const host = guessHostForUrl(url);
          const patch: any = {
            videourl: url,
            video_host: host,
            video_public_id: null,
            video_uploaded_at: new Date().toISOString(),
          };
          const { error: uerr } = await supabase.from("movies").update(patch).eq("id", mrow.id);
          if (uerr) return new Response(JSON.stringify({ error: uerr.message }), { status: 500 });
          try { await supabase.from("videos_sync_queue").delete().eq("movie_id", mrow.id); } catch {}
          return new Response(JSON.stringify({ ok: true, movieId: mrow.id, title: mrow.title, score: 1, host, normalizedUrl }), { status: 200 });
        }
      }
    }
  } catch {
    // ignore provider-map errors
  }

  // 2) File signature auto-match
  try {
    const sig = await computeFileSignature(url, body.headers ?? null);
    if (sig.headSha256 || (sig.sizeBytes && sig.tailSha256)) {
      let query = supabase
        .from("movie_file_signatures")
        .select("movie_id,size_bytes,head_sha256,tail_sha256")
        .limit(1);
      if (sig.sizeBytes) query = query.eq("size_bytes", sig.sizeBytes);
      if (sig.headSha256) query = query.eq("head_sha256", sig.headSha256);
      if (sig.tailSha256) query = query.eq("tail_sha256", sig.tailSha256);
      const { data: sigRow } = await query.maybeSingle();
      if (sigRow?.movie_id) {
        const { data: mrow } = await supabase
          .from("movies")
          .select("id,title,videourl")
          .eq("id", sigRow.movie_id)
          .maybeSingle();
        if (mrow) {
          if (mrow.videourl && !body.force) {
            return new Response(JSON.stringify({ 
              error: "movie_already_has_videourl", 
              movieId: mrow.id, 
              title: mrow.title,
              existingUrl: mrow.videourl,
              hint: "pass force=true to override" 
            }), { status: 409 });
          }
          const host = guessHostForUrl(url);
          const patch: any = {
            videourl: url,
            video_host: host,
            video_public_id: null,
            video_uploaded_at: new Date().toISOString(),
          };
          const { error: uerr } = await supabase.from("movies").update(patch).eq("id", mrow.id);
          if (uerr) return new Response(JSON.stringify({ error: uerr.message }), { status: 500 });
          try { await supabase.from("videos_sync_queue").delete().eq("movie_id", mrow.id); } catch {}
          return new Response(JSON.stringify({ ok: true, movieId: mrow.id, title: mrow.title, score: 1, host, normalizedUrl }), { status: 200 });
        }
      }
    }
  } catch {
    // ignore signature errors
  }

  const candidates = await fetchMoviesForMatching(tokens, year);
  const urlTokens = new Set(tokens);
  const ranked = candidates
    .map((m) => {
      const titleTokens = tokenizeTitle(m.title);
      const base = jaccard(titleTokens, urlTokens);
      const yearBoost = year && m.release_date?.startsWith(String(year)) ? 0.15 : 0;
      const penaltyAlreadyHosted = needsHosting(m.videourl, m.video_host) ? 0 : -0.1;
      const score = Math.max(0, Math.min(1, base + yearBoost + penaltyAlreadyHosted));
      return {
        movie: m,
        score,
        reasons: [
          `title_overlap:${base.toFixed(2)}`,
          year ? `year_hint:${year}` : undefined,
          !needsHosting(m.videourl, m.video_host) ? "already_hosted" : undefined,
        ].filter(Boolean),
      };
    })
    .sort((a, b) => b.score - a.score);

  const best = ranked[0];
  if (!best || best.score < minScore) {
    return new Response(JSON.stringify({ 
      error: "no_confident_match", 
      minScore, 
      bestScore: best?.score || 0,
      bestCandidate: best?.movie || null,
      normalizedUrl 
    }), { status: 404 });
  }

  const movieId = best.movie.id;
  const mrow = best.movie;

  // Check if movie already has videourl and force not set
  if (mrow.videourl && !body.force) {
    return new Response(JSON.stringify({ 
      error: "movie_already_has_videourl", 
      movieId, 
      title: mrow.title,
      existingUrl: mrow.videourl,
      hint: "pass force=true to override" 
    }), { status: 409 });
  }

  const host = guessHostForUrl(url);
  const patch: any = {
    videourl: url,
    video_host: host,
    video_public_id: null,
    video_uploaded_at: new Date().toISOString(),
  };
  if (!mrow.original_videourl) patch.original_videourl = mrow.videourl ?? null;

  const { error: uerr } = await supabase.from("movies").update(patch).eq("id", movieId);
  if (uerr) return new Response(JSON.stringify({ error: uerr.message }), { status: 500 });

  // Clean up any pending queue for this movie
  try {
    await supabase.from("videos_sync_queue").delete().eq("movie_id", movieId);
  } catch {
    // ignore if table does not exist or other non-critical errors
  }

  return new Response(JSON.stringify({ 
    ok: true, 
    movieId, 
    title: mrow.title,
    score: best.score,
    host,
    normalizedUrl 
  }), { status: 200 });
}

// ===== Admin registration endpoints =====
async function handleRegisterSignature(req: Request) {
  const body = (await req.json().catch(() => ({}))) as RegisterSignatureBody;
  if (!body.movieId || !body.url) return new Response(JSON.stringify({ error: "missing_params" }), { status: 400 });
  try {
    const sig = await computeFileSignature(body.url, body.headers ?? null);
    if (!sig.headSha256 && !sig.tailSha256) {
      return new Response(JSON.stringify({ error: "signature_unavailable" }), { status: 422 });
    }
    const payload: any = {
      movie_id: body.movieId,
      size_bytes: sig.sizeBytes,
      head_sha256: sig.headSha256,
      tail_sha256: sig.tailSha256,
    };
    // Upsert-like behavior: rely on unique composite index if present
    const { error } = await supabase.from("movie_file_signatures").insert(payload).select().single();
    if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });
    return new Response(JSON.stringify({ ok: true, movieId: body.movieId, signature: payload }), { status: 200 });
  } catch (e: any) {
    return new Response(JSON.stringify({ error: String(e?.message || e) }), { status: 500 });
  }
}

async function handleRegisterProviderId(req: Request) {
  const body = (await req.json().catch(() => ({}))) as RegisterProviderMapBody;
  if (!body.movieId) return new Response(JSON.stringify({ error: "missing_movieId" }), { status: 400 });
  let providerHost = body.providerHost ?? null;
  let providerId = body.providerId ?? null;
  if (body.url && (!providerHost || !providerId)) {
    const pid = extractProviderIdFromUrl(body.url);
    if (pid) { providerHost = pid.providerHost; providerId = pid.providerId; }
  }
  if (!providerHost || !providerId) return new Response(JSON.stringify({ error: "missing_provider_info" }), { status: 400 });
  try {
    const payload = { provider_host: providerHost, provider_id: providerId, movie_id: body.movieId };
    const { error } = await supabase.from("external_stream_map").insert(payload).select().single();
    if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });
    return new Response(JSON.stringify({ ok: true, movieId: body.movieId, providerHost, providerId }), { status: 200 });
  } catch (e: any) {
    return new Response(JSON.stringify({ error: String(e?.message || e) }), { status: 500 });
  }
}

Deno.serve(async (req) => {
  const url = new URL(req.url);
  const rawPath = url.pathname;
  const path = rawPath.replace(/\/+$/g, "");
  // lightweight diagnostics
  try { console.log("[video_hosting_worker]", req.method, rawPath); } catch {}

  // Public health check
  if (req.method === "GET") return new Response("OK", { status: 200 });

  // Auth for mutating endpoints
  const hdr = req.headers.get("x-admin-token") ?? "";
  const okAuth = !!ADMIN_TOKEN && hdr === ADMIN_TOKEN;
  if (!okAuth) return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401 });

  const isPreview = path.endsWith("/link/preview") || path.includes("/video_hosting_worker/link/preview");
  const isCommit = path.endsWith("/link/commit") || path.includes("/video_hosting_worker/link/commit");
  const isBulkCommit = path.endsWith("/link/bulk-commit") || path.includes("/video_hosting_worker/link/bulk-commit");
  const isAuto = path.endsWith("/link/auto") || path.includes("/video_hosting_worker/link/auto");
  const isRegisterSig = path.endsWith("/link/register-signature") || path.includes("/video_hosting_worker/link/register-signature");
  const isRegisterProvider = path.endsWith("/link/register-provider-id") || path.includes("/video_hosting_worker/link/register-provider-id");

  console.log("[DEBUG] path:", path, "isAuto:", isAuto);

  if (req.method === "POST" && isPreview) return await handleLinkPreview(req);
  if (req.method === "POST" && isCommit) return await handleLinkCommit(req);
  if (req.method === "POST" && isBulkCommit) return await handleLinkBulkCommit(req);
  if (req.method === "POST" && isAuto) {
    console.log("[DEBUG] Calling handleLinkAuto");
    return await handleLinkAuto(req);
  }
  if (req.method === "POST" && isRegisterSig) return await handleRegisterSignature(req);
  if (req.method === "POST" && isRegisterProvider) return await handleRegisterProviderId(req);

  // Unknown route
  return new Response(JSON.stringify({ error: "unknown_route", path: rawPath }), { status: 404, headers: { "Content-Type": "application/json" } });
});
