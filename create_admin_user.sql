-- SQL script to create admin users for Maqoor system
-- Run this script in your database to set up initial admin users

-- Create admin user
INSERT INTO users (name, email, password, role, department, status, created_at, updated_at) 
VALUES (
    'Admin User', 
    'admin@maqoor.com', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'ADMIN', 
    'RECEPTION', 
    'ACTIVE', 
    NOW(), 
    NOW()
);

-- Create super admin user
INSERT INTO users (name, email, password, role, department, status, created_at, updated_at) 
VALUES (
    'Super Admin', 
    'superadmin@maqoor.com', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'ADMIN', 
    'RECEPTION', 
    'ACTIVE', 
    NOW(), 
    NOW()
);

-- Create test staff user
INSERT INTO users (name, email, password, role, department, status, created_at, updated_at) 
VALUES (
    'Test Staff', 
    'staff@maqoor.com', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'USER', 
    'RECEPTION', 
    'ACTIVE', 
    NOW(), 
    NOW()
);

-- Verify the users were created
SELECT id, name, email, role, department, status, created_at FROM users;
