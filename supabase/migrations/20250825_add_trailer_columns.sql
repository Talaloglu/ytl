-- Add trailer columns to movies table if they don't exist
DO $$
BEGIN
  -- trailer_key
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'movies' AND column_name = 'trailer_key'
  ) THEN
    ALTER TABLE public.movies ADD COLUMN trailer_key TEXT;
  END IF;

  -- trailer_site
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'movies' AND column_name = 'trailer_site'
  ) THEN
    ALTER TABLE public.movies ADD COLUMN trailer_site TEXT;
  END IF;

  -- trailer_url
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'movies' AND column_name = 'trailer_url'
  ) THEN
    ALTER TABLE public.movies ADD COLUMN trailer_url TEXT;
  END IF;
END $$;
