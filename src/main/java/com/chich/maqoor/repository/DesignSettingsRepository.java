package com.chich.maqoor.repository;

import com.chich.maqoor.entity.DesignSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignSettingsRepository extends JpaRepository<DesignSettings, Long> {
    
    // Find the currently active theme
    Optional<DesignSettings> findByIsActiveTrue();
    
    // Find the default theme
    Optional<DesignSettings> findByIsDefaultTrue();
    
    // Find all themes ordered by creation date
    List<DesignSettings> findAllByOrderByCreatedAtDesc();
    
    // Find theme by name
    Optional<DesignSettings> findByThemeName(String themeName);
    
    // Check if theme name exists (excluding current theme)
    @Query("SELECT COUNT(d) > 0 FROM DesignSettings d WHERE d.themeName = :themeName AND d.id != :excludeId")
    boolean existsByThemeNameAndIdNot(@Param("themeName") String themeName, @Param("excludeId") Long excludeId);
    
    // Check if theme name exists
    boolean existsByThemeName(String themeName);
    
    // Find all active themes (should be only one, but for safety)
    List<DesignSettings> findByIsActiveTrueOrderByCreatedAtDesc();
}

