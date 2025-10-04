-- Migration: Add DB-level safeguards for direct-linking video URLs
-- - Partial unique index on movies.videourl (non-null)
-- - Optional stream_fingerprint column and index
-- This migration is defensive: it only creates the unique index if no duplicates exist.

-- 1) Add optional stream_fingerprint column
ALTER TABLE public.movies
  ADD COLUMN IF NOT EXISTS stream_fingerprint text;

-- 2) Index stream_fingerprint for faster lookups (non-unique)
CREATE INDEX IF NOT EXISTS movies_stream_fingerprint_idx
  ON public.movies (stream_fingerprint)
  WHERE stream_fingerprint IS NOT NULL;

-- 3) Create a partial unique index on videourl where NOT NULL
-- Guard against failure if duplicates currently exist by checking first.
DO $$
BEGIN
  -- Ensure the videourl column exists
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'movies'
      AND column_name = 'videourl'
  ) THEN
    -- Only create the unique index if there are no duplicates among non-null values
    IF NOT EXISTS (
      SELECT videourl
      FROM public.movies
      WHERE videourl IS NOT NULL
      GROUP BY videourl
      HAVING COUNT(*) > 1
    ) THEN
      EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS movies_videourl_unique_nonnull
               ON public.movies (videourl)
               WHERE videourl IS NOT NULL';
    ELSE
      RAISE NOTICE 'Skipped creating unique index on movies.videourl because duplicates exist. Clean data then re-run.';
    END IF;
  END IF;
END
$$;
