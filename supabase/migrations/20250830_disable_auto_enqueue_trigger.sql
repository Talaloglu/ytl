-- Disable automatic enqueue from DB trigger; let maintenance handle enqueueing
-- Safe to run multiple times.

-- Drop trigger that auto-enqueues on insert/update
DROP TRIGGER IF EXISTS trg_movies_enqueue ON public.movies;

-- Drop trigger function
DROP FUNCTION IF EXISTS public.enqueue_movie_sync();
