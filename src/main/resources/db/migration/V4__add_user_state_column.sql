-- Add state column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS state VARCHAR(255);

-- Set default state to ACTIVE for existing users
UPDATE users SET state = 'ACTIVE' WHERE state IS NULL;

-- Make state column not null after setting defaults
ALTER TABLE users ALTER COLUMN state SET NOT NULL;
