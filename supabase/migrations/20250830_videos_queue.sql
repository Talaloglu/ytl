-- Migration: videos hosting sync queue and movie columns for R2 hosting
-- Safe to run multiple times

create extension if not exists pgcrypto;

-- Queue for video hosting jobs
create table if not exists public.videos_sync_queue (
  id uuid primary key default gen_random_uuid(),
  movie_id text not null,
  source_url text,
  headers_json jsonb,
  resolver_kind text,            -- e.g., direct | provider_x
  resolver_payload jsonb,
  reason text not null,          -- unhosted | rehost_request | retry
  try_count int not null default 0,
  next_try_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index if not exists idx_videos_sync_queue_next_try on public.videos_sync_queue (next_try_at, created_at);
create index if not exists idx_videos_sync_queue_movie on public.videos_sync_queue (movie_id);

-- Movie columns to track hosting metadata
alter table public.movies add column if not exists original_videourl text;
alter table public.movies add column if not exists video_host text;             -- e.g., r2
alter table public.movies add column if not exists video_public_id text;       -- object key/id on host
alter table public.movies add column if not exists video_uploaded_at timestamptz;
