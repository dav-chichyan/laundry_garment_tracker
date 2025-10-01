-- Create design_settings table for theme management
CREATE TABLE IF NOT EXISTS design_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    theme_name VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Color scheme
    primary_color VARCHAR(7) DEFAULT '#007bff',
    secondary_color VARCHAR(7) DEFAULT '#6c757d',
    success_color VARCHAR(7) DEFAULT '#28a745',
    warning_color VARCHAR(7) DEFAULT '#ffc107',
    danger_color VARCHAR(7) DEFAULT '#dc3545',
    info_color VARCHAR(7) DEFAULT '#17a2b8',
    light_color VARCHAR(7) DEFAULT '#f8f9fa',
    dark_color VARCHAR(7) DEFAULT '#343a40',
    
    -- Header colors
    header_bg_color VARCHAR(7) DEFAULT '#FF8871',
    header_gradient_color VARCHAR(7) DEFAULT '#e67e5f',
    
    -- Sidebar colors
    sidebar_bg_color VARCHAR(7) DEFAULT '#2c3e50',
    sidebar_text_color VARCHAR(7) DEFAULT '#ffffff',
    sidebar_hover_color VARCHAR(7) DEFAULT '#34495e',
    
    -- Card colors
    card_bg_color VARCHAR(7) DEFAULT '#ffffff',
    card_header_color VARCHAR(7) DEFAULT '#97C0B4',
    
    -- Typography
    font_family VARCHAR(100) DEFAULT 'Arial, sans-serif',
    font_size_base INT DEFAULT 14,
    font_size_large INT DEFAULT 18,
    font_size_small INT DEFAULT 12,
    
    -- Layout
    border_radius INT DEFAULT 4,
    box_shadow VARCHAR(200) DEFAULT '0 0.125rem 0.25rem rgba(0, 0, 0, 0.075)',
    animation_duration INT DEFAULT 300,
    
    -- Logo settings
    show_logo BOOLEAN DEFAULT TRUE,
    logo_width INT DEFAULT 150,
    logo_height INT DEFAULT 50,
    
    -- Additional settings
    custom_css TEXT,
    description TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_design_settings_theme_name ON design_settings(theme_name);
CREATE INDEX IF NOT EXISTS idx_design_settings_is_active ON design_settings(is_active);
CREATE INDEX IF NOT EXISTS idx_design_settings_is_default ON design_settings(is_default);

-- Insert default theme
INSERT INTO design_settings (
    theme_name, 
    description, 
    is_active, 
    is_default,
    primary_color,
    secondary_color,
    success_color,
    warning_color,
    danger_color,
    info_color,
    light_color,
    dark_color,
    header_bg_color,
    header_gradient_color,
    sidebar_bg_color,
    sidebar_text_color,
    sidebar_hover_color,
    card_bg_color,
    card_header_color,
    font_family,
    font_size_base,
    font_size_large,
    font_size_small,
    border_radius,
    box_shadow,
    animation_duration,
    show_logo,
    logo_width,
    logo_height
) VALUES (
    'Default Theme',
    'System default theme with professional colors',
    TRUE,
    TRUE,
    '#007bff',
    '#6c757d',
    '#28a745',
    '#ffc107',
    '#dc3545',
    '#17a2b8',
    '#f8f9fa',
    '#343a40',
    '#FF8871',
    '#e67e5f',
    '#2c3e50',
    '#ffffff',
    '#34495e',
    '#ffffff',
    '#97C0B4',
    'Arial, sans-serif',
    14,
    18,
    12,
    4,
    '0 0.125rem 0.25rem rgba(0, 0, 0, 0.075)',
    300,
    TRUE,
    150,
    50
);

