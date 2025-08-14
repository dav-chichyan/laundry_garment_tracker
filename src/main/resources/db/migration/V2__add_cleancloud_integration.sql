-- Migration: Add CleanCloud integration fields
-- Version: V2
-- Description: Add fields for CleanCloud order and garment synchronization

-- Add CleanCloud fields to orders table
ALTER TABLE orders 
ADD COLUMN cleancloud_order_id INTEGER UNIQUE,
ADD COLUMN order_number VARCHAR(255),
ADD COLUMN customer_name VARCHAR(255),
ADD COLUMN customer_phone VARCHAR(50),
ADD COLUMN pickup_date VARCHAR(100),
ADD COLUMN delivery_date VARCHAR(100),
ADD COLUMN status VARCHAR(100),
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Add CleanCloud fields to garments table
ALTER TABLE garments 
ADD COLUMN cleancloud_garment_id VARCHAR(255),
ADD COLUMN type VARCHAR(100),
ADD COLUMN color VARCHAR(100),
ADD COLUMN size VARCHAR(50),
ADD COLUMN special_instructions TEXT,
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Create indexes for better performance
CREATE INDEX idx_orders_cleancloud_id ON orders(cleancloud_order_id);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_garments_cleancloud_id ON garments(cleancloud_garment_id);
CREATE INDEX idx_garments_order_id ON garments(orders_id);

-- Update existing records to have default values
UPDATE orders SET 
    order_number = CONCAT('ORD-', order_id),
    customer_name = 'Unknown Customer',
    status = 'PENDING'
WHERE order_number IS NULL;

UPDATE garments SET 
    type = 'Unknown',
    color = 'Unknown',
    size = 'Unknown'
WHERE type IS NULL;
