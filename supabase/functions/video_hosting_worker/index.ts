/// <reference lib="dom" />
// Deno Deploy Edge Function: video_hosting_worker
// Purpose: Orchestrate video hosting jobs to move token-protected MP4/MKV to R2 and update movies.videourl.
// Notes: Heavy downloading/uploading is expected to be performed by a GitHub Actions runner.
// Routes:
//   POST /maintenance       -> enqueue unhosted videos (scan movies)
//   POST /lease             -> lease N due jobs to a runner
//   POST /complete          -> mark job done and update movies row with hosted URL
//   POST /reschedule        -> backoff and retry later with reason
// Auth: x-admin-token header must equal ADMIN_TOKEN

// @ts-ignore
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
// eslint-disable-next-line @typescript-eslint/no-explicit-any
declare const Deno: any;

const SUPABASE_URL =
  Deno.env.get("SUPABASE_URL") ||
  Deno.env.get("SB_URL") ||
  Deno.env.get("SUPABASE_PROJECT_URL");
const SERVICE_ROLE =
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ||
  Deno.env.get("SERVICE_ROLE") ||
  Deno.env.get("SB_SERVICE_ROLE_KEY");
const ADMIN_TOKEN = Deno.env.get("ADMIN_TOKEN");

// Target hosting
const TARGET_HOST = "r2"; // stored in movies.video_host
const R2_PUBLIC_BASE_URL = (Deno.env.get("R2_PUBLIC_BASE_URL") || "").replace(/\/$/, "");

if (!SUPABASE_URL || !SERVICE_ROLE || !ADMIN_TOKEN) {
  console.error("Missing required env for video_hosting_worker.");
}

const supabase = createClient(SUPABASE_URL!, SERVICE_ROLE!);

type VideoJob = {
  id: string;
  movie_id: string;
  source_url?: string | null;
  headers_json?: any | null;
  resolver_kind?: string | null;
  resolver_payload?: any | null;
  try_count: number;
  next_try_at: string;
  reason?: string | null;
};

function needsHosting(url: string | null | undefined, host: string | null | undefined): boolean {
  if (!url) return false; // nothing to host
  if (host !== TARGET_HOST) return true;
  if (R2_PUBLIC_BASE_URL && !url.startsWith(R2_PUBLIC_BASE_URL)) return true;
  return false;
}

function computeBackoff(nextTryCount: number): string {
  const minutes = Math.min(120, Math.pow(2, Math.max(0, nextTryCount))); // up to 2h
  const d = new Date();
  d.setMinutes(d.getMinutes() + minutes);
  return d.toISOString();
}

async function scanAndEnqueueUnhosted(limit = 300): Promise<{ scanned: number; enqueued: number }> {
  const { data, error } = await supabase
    .from("movies")
    .select("id,videourl,video_host,original_videourl")
    .order("id", { ascending: true })
    .limit(limit);
  if (error) throw error;
  let enq = 0;
  for (const row of data ?? []) {
    const r = row as any;
    if (needsHosting(r.videourl, r.video_host)) {
      // Avoid duplicates
      const { data: existing } = await supabase
        .from("videos_sync_queue")
        .select("id")
        .eq("movie_id", r.id)
        .lte("next_try_at", new Date().toISOString())
        .maybeSingle();
      if (!existing?.id) {
        const { error: ierr } = await supabase.from("videos_sync_queue").insert({
          movie_id: r.id,
          source_url: r.videourl,
          headers_json: null,
          resolver_kind: null,
          resolver_payload: null,
          reason: "unhosted",
          try_count: 0,
          next_try_at: new Date().toISOString(),
        });
        if (!ierr) enq += 1;
      }
    }
  }
  return { scanned: (data ?? []).length, enqueued: enq };
}

async function leaseJobs(limit = 3): Promise<VideoJob[]> {
  const nowIso = new Date().toISOString();
  const { data, error } = await supabase
    .from("videos_sync_queue")
    .select("id,movie_id,source_url,headers_json,resolver_kind,resolver_payload,try_count,next_try_at,reason")
    .lte("next_try_at", nowIso)
    .order("created_at", { ascending: true })
    .limit(Math.max(1, Math.min(10, limit)));
  if (error) throw error;

  const leaseUntil = new Date();
  leaseUntil.setMinutes(leaseUntil.getMinutes() + 15);
  for (const job of data ?? []) {
    await supabase
      .from("videos_sync_queue")
      .update({ next_try_at: leaseUntil.toISOString() })
      .eq("id", (job as any).id);
  }
  return (data as VideoJob[]) ?? [];
}

async function completeJob(jobId: string, payload: { movie_id: string; newUrl: string; publicId?: string | null; host?: string | null }) {
  const host = payload.host || TARGET_HOST;
  const patch: any = {
    videourl: payload.newUrl,
    video_host: host,
    video_public_id: payload.publicId ?? null,
    video_uploaded_at: new Date().toISOString(),
  };
  // Preserve original_videourl if empty
  const { data: mrow } = await supabase.from("movies").select("id,original_videourl,videourl").eq("id", payload.movie_id).maybeSingle();
  if (!mrow) throw new Error("movie_not_found");
  if (!mrow.original_videourl) patch.original_videourl = mrow.videourl;
  const { error: uerr } = await supabase.from("movies").update(patch).eq("id", payload.movie_id);
  if (uerr) throw uerr;
  await supabase.from("videos_sync_queue").delete().eq("id", jobId);
}

async function rescheduleJob(jobId: string, tryCount: number, reason?: string) {
  const nextTry = computeBackoff(tryCount + 1);
  const { error } = await supabase
    .from("videos_sync_queue")
    .update({ try_count: tryCount + 1, next_try_at: nextTry, reason })
    .eq("id", jobId);
  if (error) throw error;
}

async function handleMaintenance(req: Request) {
  const body = (await req.json().catch(() => ({}))) as any;
  const limit = Math.min(Math.max(body?.limit ?? 300, 1), 2000);
  const result = await scanAndEnqueueUnhosted(limit);
  return new Response(JSON.stringify(result), { status: 200 });
}

async function handleLease(req: Request) {
  const body = (await req.json().catch(() => ({}))) as any;
  const limit = Math.min(Math.max(body?.limit ?? 3, 1), 10);
  const jobs = await leaseJobs(limit);
  return new Response(JSON.stringify({ jobs }), { status: 200 });
}

async function handleComplete(req: Request) {
  const body = (await req.json().catch(() => ({}))) as any;
  const jobId = body?.jobId as string;
  const movieId = body?.movieId as string;
  const newUrl = body?.newUrl as string;
  const publicId = (body?.publicId ?? null) as string | null;
  const host = (body?.host ?? TARGET_HOST) as string;
  if (!jobId || !movieId || !newUrl) {
    return new Response(JSON.stringify({ error: "missing_params" }), { status: 400 });
  }
  await completeJob(jobId, { movie_id: movieId, newUrl, publicId, host });
  return new Response(JSON.stringify({ ok: true }), { status: 200 });
}

async function handleReschedule(req: Request) {
  const body = (await req.json().catch(() => ({}))) as any;
  const jobId = body?.jobId as string;
  const tryCount = Number(body?.tryCount ?? 0);
  const reason = body?.reason as string | undefined;
  if (!jobId) return new Response(JSON.stringify({ error: "missing_jobId" }), { status: 400 });
  await rescheduleJob(jobId, tryCount, reason);
  return new Response(JSON.stringify({ ok: true }), { status: 200 });
}

// Enqueue or update a job manually, allowing header/cookie injection
async function handleEnqueue(req: Request) {
  const body = (await req.json().catch(() => ({}))) as any;
  const movieId = body?.movieId as string;
  const sourceUrl = (body?.sourceUrl ?? null) as string | null;
  const headers = (body?.headers ?? null) as any | null;
  const resolver_kind = (body?.resolver_kind ?? null) as string | null;
  const resolver_payload = (body?.resolver_payload ?? null) as any | null;
  const reason = (body?.reason ?? "manual") as string;
  if (!movieId) return new Response(JSON.stringify({ error: "missing_movieId" }), { status: 400 });
  const nowIso = new Date().toISOString();
  // If existing job for movie, update; else insert new
  const { data: existing } = await supabase
    .from("videos_sync_queue")
    .select("id")
    .eq("movie_id", movieId)
    .maybeSingle();
  if (existing?.id) {
    const { error } = await supabase
      .from("videos_sync_queue")
      .update({
        source_url: sourceUrl,
        headers_json: headers,
        resolver_kind,
        resolver_payload,
        reason,
        try_count: 0,
        next_try_at: nowIso,
      })
      .eq("id", existing.id);
    if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });
    return new Response(JSON.stringify({ ok: true, id: existing.id, updated: true }), { status: 200 });
  } else {
    const { data, error } = await supabase
      .from("videos_sync_queue")
      .insert({
        movie_id: movieId,
        source_url: sourceUrl,
        headers_json: headers,
        resolver_kind,
        resolver_payload,
        reason,
        try_count: 0,
        next_try_at: nowIso,
      })
      .select("id")
      .maybeSingle();
    if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });
    return new Response(JSON.stringify({ ok: true, id: data?.id, created: true }), { status: 201 });
  }
}

// Resolve fresh source URL and headers for a job with 403 error handling
async function handleResolve(req: Request) {
  const body = (await req.json().catch(() => ({}))) as any;
  const { job_id, jobId, resolver_kind, resolver_payload, try_count = 0 } = body;
  const actualJobId = job_id || jobId; // Support both parameter names
  
  const { data: job, error } = await supabase
    .from("videos_sync_queue")
    .select("source_url,headers_json,movie_id")
    .eq("id", actualJobId)
    .single();
  
  if (error || !job) {
    return new Response(JSON.stringify({ error: "job_not_found" }), { status: 404 });
  }
  
  const kind = resolver_kind ?? "static";
  
  // For fatv.vip URLs, try different strategies on 403 errors
  if (job.source_url?.includes("fatv.vip")) {
    return await resolveFatvUrl(job, try_count);
  }
  
  // Default: return original URL with enhanced headers
  return new Response(JSON.stringify({
    sourceUrl: job.source_url,  // Match GitHub Actions script expectation
    headers: {
      "User-Agent": getRandomUserAgent(),
      "Referer": extractDomain(job.source_url),
      "Accept": "*/*",
      "Accept-Language": "en-US,en;q=0.9",
      "Cache-Control": "no-cache",
      "Connection": "keep-alive",
      ...(job.headers_json ?? {})
    }
  }), { status: 200 });
}

// Resolve Xtream-style URLs with 403 error handling strategies
async function resolveFatvUrl(job: any, tryCount: number) {
  const originalUrl = job.source_url;
  const movieId = job.movie_id;
  
  // Strategy 1: Header rotation for basic 403 bypass
  const userAgents = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "VLC/3.0.16 LibVLC/3.0.16",
    "FFmpeg/4.4.2"
  ];
  
  const referers = [
    "https://fatv.vip/",
    "https://www.fatv.vip/",
    "https://fatv.vip/movie/",
    "https://fatv.vip/watch/",
    "" // No referer
  ];
  
  const userAgent = userAgents[tryCount % userAgents.length];
  const referer = referers[tryCount % referers.length];
  
  // Strategy 2: Check for updated URL in database (URL structure changed)
  if (tryCount >= 2) {
    const { data: movie } = await supabase
      .from("movies")
      .select("videourl")
      .eq("id", movieId)
      .single();
    
    if (movie?.videourl && movie.videourl !== originalUrl) {
      // Database has newer URL - use it
      return new Response(JSON.stringify({
        sourceUrl: movie.videourl,  // Match GitHub Actions script expectation
        headers: {
          "User-Agent": userAgent,
          "Referer": referer || undefined,
          "Accept": "*/*",
          "Accept-Language": "en-US,en;q=0.9"
        }
      }), { status: 200 });
    }
  }
  
  // Strategy 3: Try alternative domains/mirrors (if provider has them)
  if (tryCount >= 4) {
    const alternativeUrl = originalUrl.replace("1.fatv.vip", "2.fatv.vip")
                                    .replace("http://", "https://");
    
    return new Response(JSON.stringify({
      sourceUrl: alternativeUrl,  // Match GitHub Actions script expectation
      headers: {
        "User-Agent": userAgent,
        "Referer": referer || undefined,
        "Accept": "*/*",
        "Accept-Language": "en-US,en;q=0.9"
      }
    }), { status: 200 });
  }
  
  // Default: Original URL with rotating headers
  const headers: any = {
    "User-Agent": userAgent,
    "Accept": "*/*",
    "Accept-Language": "en-US,en;q=0.9",
    "Cache-Control": "no-cache",
    "Connection": "keep-alive"
  };
  
  if (referer) headers["Referer"] = referer;
  
  return new Response(JSON.stringify({
    sourceUrl: originalUrl,  // Match GitHub Actions script expectation
    headers
  }), { status: 200 });
}

// Helper functions
function getRandomUserAgent() {
  const agents = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
  ];
  return agents[Math.floor(Math.random() * agents.length)];
}

function extractDomain(url: string): string {
  try {
    const u = new URL(url);
    return `${u.protocol}//${u.hostname}/`;
  } catch {
    return "https://fatv.vip/";
  }
}

// Clear all jobs from the queue (for testing/reset)
async function handleClear(req: Request) {
  const { error } = await supabase.from("videos_sync_queue").delete().neq("id", "00000000-0000-0000-0000-000000000000");
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  return new Response(JSON.stringify({ ok: true, cleared: true }), { status: 200 });
}

// Bulk enqueue all movies with videourl that need hosting
async function handleBulkEnqueue(req: Request) {
  const body = (await req.json().catch(() => ({}))) as any;
  const limit = Math.min(Math.max(body?.limit ?? 1000, 1), 5000);
  const { data, error } = await supabase
    .from("movies")
    .select("id,videourl,video_host")
    .not("videourl", "is", null)
    .neq("videourl", "")
    .order("id", { ascending: true })
    .limit(limit);
  if (error) throw error;
  
  let enqueued = 0;
  const nowIso = new Date().toISOString();
  
  for (const row of data ?? []) {
    const r = row as any;
    if (needsHosting(r.videourl, r.video_host)) {
      // Check if already queued
      const { data: existing } = await supabase
        .from("videos_sync_queue")
        .select("id")
        .eq("movie_id", r.id)
        .maybeSingle();
      
      if (!existing?.id) {
        const { error: ierr } = await supabase.from("videos_sync_queue").insert({
          movie_id: r.id,
          source_url: r.videourl,
          headers_json: {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Referer": "https://fatv.vip/",
            "Accept": "*/*",
            "Accept-Language": "en-US,en;q=0.9"
          },
          resolver_kind: null,
          resolver_payload: null,
          reason: "bulk_unhosted",
          try_count: 0,
          next_try_at: nowIso,
        });
        if (!ierr) enqueued += 1;
      }
    }
  }
  
  return new Response(JSON.stringify({ scanned: (data ?? []).length, enqueued }), { status: 200 });
}

Deno.serve(async (req) => {
  const hdr = req.headers.get("x-admin-token") ?? "";
  const okAuth = ADMIN_TOKEN && hdr && hdr === ADMIN_TOKEN;
  if (!okAuth) {
    return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401 });
  }
  const url = new URL(req.url);
  if (req.method === "POST" && url.pathname.endsWith("/maintenance")) return await handleMaintenance(req);
  if (req.method === "POST" && url.pathname.endsWith("/enqueue")) return await handleEnqueue(req);
  if (req.method === "POST" && url.pathname.endsWith("/lease")) return await handleLease(req);
  if (req.method === "POST" && url.pathname.endsWith("/complete")) return await handleComplete(req);
  if (req.method === "POST" && url.pathname.endsWith("/reschedule")) return await handleReschedule(req);
  if (req.method === "POST" && url.pathname.endsWith("/resolve")) return await handleResolve(req);
  if (req.method === "POST" && url.pathname.endsWith("/clear")) return await handleClear(req);
  if (req.method === "POST" && url.pathname.endsWith("/bulk-enqueue")) return await handleBulkEnqueue(req);
  return new Response("OK", { status: 200 });
});
