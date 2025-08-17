-- SQL script to create users for Maqoor system
-- Run this script in your database to set up initial users

-- Create users for different departments
INSERT INTO users (name, email, password, role, department, status, created_at, updated_at) 
VALUES 
    ('John Smith', 'john.smith@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'RECEPTION', 'ACTIVE', NOW(), NOW()),
    ('Sarah Johnson', 'sarah.johnson@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'WASHING', 'ACTIVE', NOW(), NOW()),
    ('Mike Davis', 'mike.davis@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'DRYING', 'ACTIVE', NOW(), NOW()),
    ('Lisa Wilson', 'lisa.wilson@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'IRONING', 'ACTIVE', NOW(), NOW()),
    ('David Brown', 'david.brown@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'PACKAGING', 'ACTIVE', NOW(), NOW()),
    ('Emma Taylor', 'emma.taylor@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'LOCKER', 'ACTIVE', NOW(), NOW()),
    ('James Miller', 'james.miller@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'RECEPTION', 'ACTIVE', NOW(), NOW()),
    ('Maria Garcia', 'maria.garcia@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'WASHING', 'ACTIVE', NOW(), NOW()),
    ('Robert Lee', 'robert.lee@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'DRYING', 'ACTIVE', NOW(), NOW()),
    ('Jennifer White', 'jennifer.white@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 'IRONING', 'ACTIVE', NOW(), NOW());

-- Create additional admin users
INSERT INTO users (name, email, password, role, department, status, created_at, updated_at) 
VALUES 
    ('Admin Manager', 'manager@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 'RECEPTION', 'ACTIVE', NOW(), NOW()),
    ('System Admin', 'system@maqoor.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 'RECEPTION', 'ACTIVE', NOW(), NOW());

-- Verify the users were created
SELECT id, name, email, role, department, status, created_at FROM users ORDER BY created_at DESC;
