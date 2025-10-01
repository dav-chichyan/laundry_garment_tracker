package com.chich.maqoor.service;

import com.chich.maqoor.entity.DesignSettings;

import java.util.List;

public interface DesignService {
    
    // Get all themes
    List<DesignSettings> getAllThemes();
    
    // Get the currently active theme
    DesignSettings getActiveTheme();
    
    // Get theme by ID
    DesignSettings getThemeById(Long id);
    
    // Get theme by name
    DesignSettings getThemeByName(String themeName);
    
    // Create a new theme
    DesignSettings createTheme(DesignSettings theme);
    
    // Update an existing theme
    DesignSettings updateTheme(Long id, DesignSettings theme);
    
    // Delete a theme
    void deleteTheme(Long id);
    
    // Activate a theme (deactivates all others)
    DesignSettings activateTheme(Long id);
    
    // Set default theme
    DesignSettings setDefaultTheme(Long id);
    
    // Duplicate a theme
    DesignSettings duplicateTheme(Long id, String newThemeName);
    
    // Reset to default theme
    DesignSettings resetToDefault();
    
    // Get CSS variables for the active theme
    String getActiveThemeCss();
    
    // Validate theme name uniqueness
    boolean isThemeNameUnique(String themeName, Long excludeId);
    
    // Check if theme name exists
    boolean themeNameExists(String themeName);
}

