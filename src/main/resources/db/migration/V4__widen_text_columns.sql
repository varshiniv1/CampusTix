-- Widen description to TEXT (AI-generated content exceeds VARCHAR(255))
-- Widen image_url to handle long CDN/Wikipedia URLs
ALTER TABLE events ALTER COLUMN description TYPE TEXT;
ALTER TABLE events ALTER COLUMN image_url   TYPE VARCHAR(2048);
