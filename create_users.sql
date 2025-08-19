-- SQL script to create users for all departments
-- This will be used to test the garment scanning system

-- First, let's check what users exist
SELECT * FROM users;

-- Insert users for all departments
INSERT INTO users (name, email, password, role, state, status, username, department) VALUES
('Admin User', 'admin@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'ADMIN', 'ACTIVE', 'ACTIVE', 'admin@maqoor.com', 'RECEPTION'),
('John Reception', 'john@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'john@maqoor.com', 'RECEPTION'),
('Sarah Examination', 'sarah@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'sarah@maqoor.com', 'EXAMINATION'),
('Mike Washing', 'mike@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'mike@maqoor.com', 'WASHING'),
('Lisa Stain Removal', 'lisa@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'lisa@maqoor.com', 'STAIN_REMOVAL'),
('David Dry Cleaning', 'david@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'david@maqoor.com', 'DRY_CLEANING'),
('Emma Shoes', 'emma@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'emma@maqoor.com', 'SHOES'),
('Tom Ironing', 'tom@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'tom@maqoor.com', 'IRONING'),
('Anna Packaging', 'anna@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'anna@maqoor.com', 'PACKAGING'),
('Chris Delivery', 'chris@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'chris@maqoor.com', 'DELIVERY'),
('Maria Locker', 'maria@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'maria@maqoor.com', 'LOCKER'),
('Alex Alteration', 'alex@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'alex@maqoor.com', 'ALTERATION'),
('Sophie Finishing', 'sophie@maqoor.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'ACTIVE', 'ACTIVE', 'sophie@maqoor.com', 'FINISHING');

-- Verify users were created
SELECT name, email, department FROM users ORDER BY department;
