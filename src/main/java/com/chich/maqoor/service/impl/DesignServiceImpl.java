package com.chich.maqoor.service.impl;

import com.chich.maqoor.entity.DesignSettings;
import com.chich.maqoor.repository.DesignSettingsRepository;
import com.chich.maqoor.service.DesignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignServiceImpl implements DesignService {
    
    private final DesignSettingsRepository designSettingsRepository;
    
    @Override
    public List<DesignSettings> getAllThemes() {
        return designSettingsRepository.findAllByOrderByCreatedAtDesc();
    }
    
    @Override
    public DesignSettings getActiveTheme() {
        return designSettingsRepository.findByIsActiveTrue()
                .orElseGet(this::getDefaultTheme);
    }
    
    private DesignSettings getDefaultTheme() {
        return designSettingsRepository.findByIsDefaultTrue()
                .orElseGet(this::createDefaultTheme);
    }
    
    private DesignSettings createDefaultTheme() {
        log.info("Creating default theme as none exists");
        DesignSettings defaultTheme = DesignSettings.builder()
                .themeName("Default Theme")
                .description("System default theme")
                .isActive(true)
                .isDefault(true)
                .primaryColor("#007bff")
                .secondaryColor("#6c757d")
                .successColor("#28a745")
                .warningColor("#ffc107")
                .dangerColor("#dc3545")
                .infoColor("#17a2b8")
                .lightColor("#f8f9fa")
                .darkColor("#343a40")
                .headerBgColor("#FF8871")
                .headerGradientColor("#e67e5f")
                .sidebarBgColor("#2c3e50")
                .sidebarTextColor("#ffffff")
                .sidebarHoverColor("#34495e")
                .cardBgColor("#ffffff")
                .cardHeaderColor("#97C0B4")
                .fontFamily("Arial, sans-serif")
                .fontSizeBase(14)
                .fontSizeLarge(18)
                .fontSizeSmall(12)
                .borderRadius(4)
                .boxShadow("0 0.125rem 0.25rem rgba(0, 0, 0, 0.075)")
                .animationDuration(300)
                .showLogo(true)
                .logoWidth(150)
                .logoHeight(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return designSettingsRepository.save(defaultTheme);
    }
    
    @Override
    public DesignSettings getThemeById(Long id) {
        return designSettingsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Theme not found with id: " + id));
    }
    
    @Override
    public DesignSettings getThemeByName(String themeName) {
        return designSettingsRepository.findByThemeName(themeName)
                .orElseThrow(() -> new RuntimeException("Theme not found with name: " + themeName));
    }
    
    @Override
    @Transactional
    public DesignSettings createTheme(DesignSettings theme) {
        if (themeNameExists(theme.getThemeName())) {
            throw new RuntimeException("Theme name already exists: " + theme.getThemeName());
        }
        
        theme.setIsActive(false);
        theme.setIsDefault(false);
        theme.setCreatedAt(LocalDateTime.now());
        theme.setUpdatedAt(LocalDateTime.now());
        
        return designSettingsRepository.save(theme);
    }
    
    @Override
    @Transactional
    public DesignSettings updateTheme(Long id, DesignSettings theme) {
        DesignSettings existingTheme = getThemeById(id);
        
        if (!existingTheme.getThemeName().equals(theme.getThemeName()) && 
            themeNameExists(theme.getThemeName())) {
            throw new RuntimeException("Theme name already exists: " + theme.getThemeName());
        }
        
        // Update fields
        existingTheme.setThemeName(theme.getThemeName());
        existingTheme.setDescription(theme.getDescription());
        existingTheme.setPrimaryColor(theme.getPrimaryColor());
        existingTheme.setSecondaryColor(theme.getSecondaryColor());
        existingTheme.setSuccessColor(theme.getSuccessColor());
        existingTheme.setWarningColor(theme.getWarningColor());
        existingTheme.setDangerColor(theme.getDangerColor());
        existingTheme.setInfoColor(theme.getInfoColor());
        existingTheme.setLightColor(theme.getLightColor());
        existingTheme.setDarkColor(theme.getDarkColor());
        existingTheme.setHeaderBgColor(theme.getHeaderBgColor());
        existingTheme.setHeaderGradientColor(theme.getHeaderGradientColor());
        existingTheme.setSidebarBgColor(theme.getSidebarBgColor());
        existingTheme.setSidebarTextColor(theme.getSidebarTextColor());
        existingTheme.setSidebarHoverColor(theme.getSidebarHoverColor());
        existingTheme.setCardBgColor(theme.getCardBgColor());
        existingTheme.setCardHeaderColor(theme.getCardHeaderColor());
        existingTheme.setFontFamily(theme.getFontFamily());
        existingTheme.setFontSizeBase(theme.getFontSizeBase());
        existingTheme.setFontSizeLarge(theme.getFontSizeLarge());
        existingTheme.setFontSizeSmall(theme.getFontSizeSmall());
        existingTheme.setBorderRadius(theme.getBorderRadius());
        existingTheme.setBoxShadow(theme.getBoxShadow());
        existingTheme.setAnimationDuration(theme.getAnimationDuration());
        existingTheme.setShowLogo(theme.getShowLogo());
        existingTheme.setLogoWidth(theme.getLogoWidth());
        existingTheme.setLogoHeight(theme.getLogoHeight());
        existingTheme.setCustomCss(theme.getCustomCss());
        existingTheme.setUpdatedAt(LocalDateTime.now());
        
        return designSettingsRepository.save(existingTheme);
    }
    
    @Override
    @Transactional
    public void deleteTheme(Long id) {
        DesignSettings theme = getThemeById(id);
        if (theme.getIsDefault()) {
            throw new RuntimeException("Cannot delete the default theme");
        }
        
        if (theme.getIsActive()) {
            // If deleting active theme, activate the default theme
            resetToDefault();
        }
        
        designSettingsRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public DesignSettings activateTheme(Long id) {
        // Deactivate all themes
        List<DesignSettings> activeThemes = designSettingsRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        for (DesignSettings activeTheme : activeThemes) {
            activeTheme.setIsActive(false);
            designSettingsRepository.save(activeTheme);
        }
        
        // Activate the selected theme
        DesignSettings theme = getThemeById(id);
        theme.setIsActive(true);
        theme.setUpdatedAt(LocalDateTime.now());
        
        return designSettingsRepository.save(theme);
    }
    
    @Override
    @Transactional
    public DesignSettings setDefaultTheme(Long id) {
        // Remove default from all themes
        List<DesignSettings> allThemes = designSettingsRepository.findAll();
        for (DesignSettings theme : allThemes) {
            theme.setIsDefault(false);
            designSettingsRepository.save(theme);
        }
        
        // Set new default
        DesignSettings theme = getThemeById(id);
        theme.setIsDefault(true);
        theme.setUpdatedAt(LocalDateTime.now());
        
        return designSettingsRepository.save(theme);
    }
    
    @Override
    @Transactional
    public DesignSettings duplicateTheme(Long id, String newThemeName) {
        if (themeNameExists(newThemeName)) {
            throw new RuntimeException("Theme name already exists: " + newThemeName);
        }
        
        DesignSettings originalTheme = getThemeById(id);
        
        DesignSettings duplicatedTheme = DesignSettings.builder()
                .themeName(newThemeName)
                .description("Copy of " + originalTheme.getThemeName())
                .isActive(false)
                .isDefault(false)
                .primaryColor(originalTheme.getPrimaryColor())
                .secondaryColor(originalTheme.getSecondaryColor())
                .successColor(originalTheme.getSuccessColor())
                .warningColor(originalTheme.getWarningColor())
                .dangerColor(originalTheme.getDangerColor())
                .infoColor(originalTheme.getInfoColor())
                .lightColor(originalTheme.getLightColor())
                .darkColor(originalTheme.getDarkColor())
                .headerBgColor(originalTheme.getHeaderBgColor())
                .headerGradientColor(originalTheme.getHeaderGradientColor())
                .sidebarBgColor(originalTheme.getSidebarBgColor())
                .sidebarTextColor(originalTheme.getSidebarTextColor())
                .sidebarHoverColor(originalTheme.getSidebarHoverColor())
                .cardBgColor(originalTheme.getCardBgColor())
                .cardHeaderColor(originalTheme.getCardHeaderColor())
                .fontFamily(originalTheme.getFontFamily())
                .fontSizeBase(originalTheme.getFontSizeBase())
                .fontSizeLarge(originalTheme.getFontSizeLarge())
                .fontSizeSmall(originalTheme.getFontSizeSmall())
                .borderRadius(originalTheme.getBorderRadius())
                .boxShadow(originalTheme.getBoxShadow())
                .animationDuration(originalTheme.getAnimationDuration())
                .showLogo(originalTheme.getShowLogo())
                .logoWidth(originalTheme.getLogoWidth())
                .logoHeight(originalTheme.getLogoHeight())
                .customCss(originalTheme.getCustomCss())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return designSettingsRepository.save(duplicatedTheme);
    }
    
    @Override
    @Transactional
    public DesignSettings resetToDefault() {
        DesignSettings defaultTheme = designSettingsRepository.findByIsDefaultTrue()
                .orElseGet(this::createDefaultTheme);
        
        return activateTheme(defaultTheme.getId());
    }
    
    @Override
    public String getActiveThemeCss() {
        DesignSettings activeTheme = getActiveTheme();
        
        StringBuilder css = new StringBuilder();
        css.append(":root {\n");
        css.append("  --primary-color: ").append(activeTheme.getPrimaryColor()).append(";\n");
        css.append("  --secondary-color: ").append(activeTheme.getSecondaryColor()).append(";\n");
        css.append("  --success-color: ").append(activeTheme.getSuccessColor()).append(";\n");
        css.append("  --warning-color: ").append(activeTheme.getWarningColor()).append(";\n");
        css.append("  --danger-color: ").append(activeTheme.getDangerColor()).append(";\n");
        css.append("  --info-color: ").append(activeTheme.getInfoColor()).append(";\n");
        css.append("  --light-color: ").append(activeTheme.getLightColor()).append(";\n");
        css.append("  --dark-color: ").append(activeTheme.getDarkColor()).append(";\n");
        css.append("  --header-bg-color: ").append(activeTheme.getHeaderBgColor()).append(";\n");
        css.append("  --header-gradient-color: ").append(activeTheme.getHeaderGradientColor()).append(";\n");
        css.append("  --sidebar-bg-color: ").append(activeTheme.getSidebarBgColor()).append(";\n");
        css.append("  --sidebar-text-color: ").append(activeTheme.getSidebarTextColor()).append(";\n");
        css.append("  --sidebar-hover-color: ").append(activeTheme.getSidebarHoverColor()).append(";\n");
        css.append("  --card-bg-color: ").append(activeTheme.getCardBgColor()).append(";\n");
        css.append("  --card-header-color: ").append(activeTheme.getCardHeaderColor()).append(";\n");
        css.append("  --font-family: ").append(activeTheme.getFontFamily()).append(";\n");
        css.append("  --font-size-base: ").append(activeTheme.getFontSizeBase()).append("px;\n");
        css.append("  --font-size-large: ").append(activeTheme.getFontSizeLarge()).append("px;\n");
        css.append("  --font-size-small: ").append(activeTheme.getFontSizeSmall()).append("px;\n");
        css.append("  --border-radius: ").append(activeTheme.getBorderRadius()).append("px;\n");
        css.append("  --box-shadow: ").append(activeTheme.getBoxShadow()).append(";\n");
        css.append("  --animation-duration: ").append(activeTheme.getAnimationDuration()).append("ms;\n");
        css.append("}\n");
        
        // Add custom CSS if provided
        if (activeTheme.getCustomCss() != null && !activeTheme.getCustomCss().trim().isEmpty()) {
            css.append("\n").append(activeTheme.getCustomCss());
        }
        
        return css.toString();
    }
    
    @Override
    public boolean isThemeNameUnique(String themeName, Long excludeId) {
        return !designSettingsRepository.existsByThemeNameAndIdNot(themeName, excludeId);
    }
    
    @Override
    public boolean themeNameExists(String themeName) {
        return designSettingsRepository.existsByThemeName(themeName);
    }
}

