-- Create user_departments mapping table for many-to-many relationship
CREATE TABLE IF NOT EXISTS user_departments (
    user_id INT NOT NULL,
    department_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, department_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Add index for better performance
CREATE INDEX IF NOT EXISTS idx_user_departments_user_id ON user_departments(user_id);
CREATE INDEX IF NOT EXISTS idx_user_departments_department_id ON user_departments(department_id);

-- Migrate existing single department assignments to the new mapping table
-- This preserves existing data while transitioning to the new structure
INSERT INTO user_departments (user_id, department_id)
SELECT id, department FROM users 
WHERE department IS NOT NULL AND department != '';

-- Note: We'll keep the existing department column for backward compatibility
-- but new assignments will use the mapping table
