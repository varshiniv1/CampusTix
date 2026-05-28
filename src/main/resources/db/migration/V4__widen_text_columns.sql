DO $$
BEGIN
    IF (SELECT data_type FROM information_schema.columns
        WHERE table_schema='public' AND table_name='events' AND column_name='description') = 'character varying' THEN
        ALTER TABLE events ALTER COLUMN description TYPE TEXT;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='events' AND column_name='image_url'
          AND (data_type='character varying' AND character_maximum_length < 2048)) THEN
        ALTER TABLE events ALTER COLUMN image_url TYPE VARCHAR(2048);
    END IF;

    IF (SELECT data_type FROM information_schema.columns
        WHERE table_schema='public' AND table_name='events' AND column_name='contact_info') = 'character varying' THEN
        ALTER TABLE events ALTER COLUMN contact_info TYPE TEXT;
    END IF;
END $$;
