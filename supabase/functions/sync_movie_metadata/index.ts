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

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

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

async function fetchTmdbDetailsById(id: number) {
  const url = `https://api.themoviedb.org/3/movie/${id}?api_key=${TMDB_API_KEY}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`TMDB details failed: ${res.status}`);
  return await res.json();
}

async function searchTmdbByTitle(title: string) {
  const url = `https://api.themoviedb.org/3/search/movie?api_key=${TMDB_API_KEY}&query=${encodeURIComponent(title)}&page=1&include_adult=false`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`TMDB search failed: ${res.status}`);
  return await res.json();
}

function pickBestMatchByTitle(query: string, results: any[]): any | null {
  if (!results?.length) return null;
  const q = normalizeTitle(query);
  const exact = results.find((r) => normalizeTitle(r.title || "") === q);
  if (exact) return exact;
  return results.sort((a, b) => (b.popularity ?? 0) - (a.popularity ?? 0))[0] ?? null;
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

async function syncOne(movie: MovieRow, force = false) {
  try {
    let details: any | null = null;

    if (movie.tmdb_id && !force) {
      details = await fetchTmdbDetailsById(movie.tmdb_id);
    } else {
      const search = await searchTmdbByTitle(movie.title);
      const best = pickBestMatchByTitle(movie.title, search?.results || []);
      if (!best) {
        return { ok: false, reason: "no_tmdb_match" as const };
      }
      details = await fetchTmdbDetailsById(best.id);
    }

    const patch = tmdbToUpdatePatch(details);
    const { error } = await supabase.from("movies").update(patch).eq("id", movie.id);
    if (error) throw error;

    return { ok: true as const, tmdb_id: patch.tmdb_id };
  } catch (e: any) {
    return { ok: false as const, reason: e?.message ?? "unknown_error" };
  }
}

// =================== Queue worker for continuous syncing ===================

type QueueRow = {
  id: string;
  movie_id: string;
  tmdb_id: number | null;
  try_count: number;
  next_try_at: string;
};

function computeBackoff(nextTryCount: number): string {
  // Exponential backoff in minutes, capped to 60 minutes
  const minutes = Math.min(60, Math.pow(2, Math.max(0, nextTryCount)));
  const d = new Date();
  d.setMinutes(d.getMinutes() + minutes);
  return d.toISOString();
}

async function fetchPendingJobs(limit = 50): Promise<QueueRow[]> {
  const nowIso = new Date().toISOString();
  const { data, error } = await supabase
    .from("movies_sync_queue")
    .select("id,movie_id,tmdb_id,try_count,next_try_at")
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
  const nextTryAt = computeBackoff(job.try_count + 1);
  const { error } = await supabase
    .from("movies_sync_queue")
    .update({ try_count: job.try_count + 1, next_try_at: nextTryAt, reason })
    .eq("id", job.id);
  if (error) throw error;
}

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
      const result = await syncOne(movie, false);
      if (result.ok) {
        await completeJob(job.id);
        done += 1;
      } else {
        await rescheduleJob(job, result.reason as any);
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

serve(async (req) => {
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
  if (req.method === "POST" && url.pathname.endsWith("/bulk")) {
    return await handleBulkSync(req);
  }
  if (req.method === "POST") {
    return await handleSingleSync(req);
  }
  return new Response("OK", { status: 200 });
});
