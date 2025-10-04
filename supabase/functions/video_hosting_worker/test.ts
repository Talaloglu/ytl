/// <reference lib="dom" />
// @ts-ignore
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
declare const Deno: any;

const SUPABASE_URL = Deno.env.get("SUPABASE_URL");
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
const ADMIN_TOKEN = Deno.env.get("ADMIN_TOKEN");

if (!SUPABASE_URL || !SERVICE_ROLE || !ADMIN_TOKEN) {
  console.error("Missing required env for video_hosting_worker.");
}

const supabase = createClient(SUPABASE_URL!, SERVICE_ROLE!);

Deno.serve(async (req) => {
  const url = new URL(req.url);
  const path = url.pathname.replace(/\/+$/g, "");
  
  if (req.method === "GET") return new Response("OK", { status: 200 });
  
  const hdr = req.headers.get("x-admin-token") ?? "";
  const okAuth = !!ADMIN_TOKEN && hdr === ADMIN_TOKEN;
  if (!okAuth) return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401 });
  
  if (path.endsWith("/link/auto")) {
    return new Response(JSON.stringify({ message: "auto endpoint works" }), { status: 200 });
  }
  
  return new Response(JSON.stringify({ error: "unknown_route", path }), { status: 404 });
});
