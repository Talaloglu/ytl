-- Migration: movies metadata sync queue and trigger
-- Safe to run multiple times

-- Enable extensions used for UUID and JSON ops
create extension if not exists pgcrypto;

-- Queue table to orchestrate metadata sync jobs
create table if not exists public.movies_sync_queue (
  id uuid primary key default gen_random_uuid(),
  movie_id text not null,
  tmdb_id int,
  reason text not null,                -- inserted | updated | backfill
  try_count int not null default 0,
  next_try_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

-- Helpful index for the worker polling
create index if not exists idx_movies_sync_queue_next_try on public.movies_sync_queue (next_try_at, created_at);
create index if not exists idx_movies_sync_queue_movie on public.movies_sync_queue (movie_id);

-- Trigger function: enqueue when metadata is missing
create or replace function public.enqueue_movie_sync()
returns trigger as $$
begin
  -- Only enqueue if core metadata is missing
  if (NEW.tmdb_id is null or NEW.poster_path is null or NEW.backdrop_path is null) then
    insert into public.movies_sync_queue(movie_id, tmdb_id, reason)
    values (NEW.id, NEW.tmdb_id, tg_op::text) -- inserted/updated
    on conflict do nothing;
  end if;
  return NEW;
end;
$$ language plpgsql security definer;

-- Attach trigger to movies table
drop trigger if exists trg_movies_enqueue on public.movies;
create trigger trg_movies_enqueue
after insert or update of title, tmdb_id, videourl
on public.movies
for each row execute procedure public.enqueue_movie_sync();

-- Backfill helper: view rows missing essential metadata
create or replace view public.movies_missing_metadata as
select m.*
from public.movies m
where (m.tmdb_id is null or m.poster_path is null or m.backdrop_path is null);
