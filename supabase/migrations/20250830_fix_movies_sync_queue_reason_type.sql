-- Fix movies_sync_queue.reason type to text and ensure next_try_at is timestamptz
-- Safe to run multiple times.

-- 1) Inspect current types (copy into SQL editor if you want to verify)
-- select column_name, data_type from information_schema.columns
-- where table_schema='public' and table_name='movies_sync_queue'
-- order by ordinal_position;

-- 2) If reason is NOT text, convert it to text
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='movies_sync_queue'
      AND column_name='reason' AND data_type <> 'text'
  ) THEN
    ALTER TABLE public.movies_sync_queue
      ALTER COLUMN reason TYPE text USING reason::text;
  END IF;
END$$;

-- 3) Ensure next_try_at is timestamptz (many runtimes expect full ISO timestamps)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='movies_sync_queue'
      AND column_name='next_try_at' AND data_type <> 'timestamp with time zone'
  ) THEN
    ALTER TABLE public.movies_sync_queue
      ALTER COLUMN next_try_at TYPE timestamptz USING
        (
          CASE
            WHEN pg_typeof(next_try_at)::text IN ('date') THEN (next_try_at::timestamp AT TIME ZONE 'UTC')
            WHEN pg_typeof(next_try_at)::text IN ('timestamp without time zone') THEN (next_try_at AT TIME ZONE 'UTC')
            ELSE next_try_at::timestamptz
          END
        );
  END IF;
END$$;
