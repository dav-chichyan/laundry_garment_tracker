package com.chich.maqoor.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "design_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "theme_name", nullable = false, unique = true)
    private String themeName;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
    
    // Color scheme
    @Column(name = "primary_color", length = 7)
    @Builder.Default
    private String primaryColor = "#007bff";
    
    @Column(name = "secondary_color", length = 7)
    @Builder.Default
    private String secondaryColor = "#6c757d";
    
    @Column(name = "success_color", length = 7)
    @Builder.Default
    private String successColor = "#28a745";
    
    @Column(name = "warning_color", length = 7)
    @Builder.Default
    private String warningColor = "#ffc107";
    
    @Column(name = "danger_color", length = 7)
    @Builder.Default
    private String dangerColor = "#dc3545";
    
    @Column(name = "info_color", length = 7)
    @Builder.Default
    private String infoColor = "#17a2b8";
    
    @Column(name = "light_color", length = 7)
    @Builder.Default
    private String lightColor = "#f8f9fa";
    
    @Column(name = "dark_color", length = 7)
    @Builder.Default
    private String darkColor = "#343a40";
    
    // Header colors
    @Column(name = "header_bg_color", length = 7)
    @Builder.Default
    private String headerBgColor = "#FF8871";
    
    @Column(name = "header_gradient_color", length = 7)
    @Builder.Default
    private String headerGradientColor = "#e67e5f";
    
    // Sidebar colors
    @Column(name = "sidebar_bg_color", length = 7)
    @Builder.Default
    private String sidebarBgColor = "#2c3e50";
    
    @Column(name = "sidebar_text_color", length = 7)
    @Builder.Default
    private String sidebarTextColor = "#ffffff";
    
    @Column(name = "sidebar_hover_color", length = 7)
    @Builder.Default
    private String sidebarHoverColor = "#34495e";
    
    // Card colors
    @Column(name = "card_bg_color", length = 7)
    @Builder.Default
    private String cardBgColor = "#ffffff";
    
    @Column(name = "card_header_color", length = 7)
    @Builder.Default
    private String cardHeaderColor = "#97C0B4";
    
    // Typography
    @Column(name = "font_family", length = 100)
    @Builder.Default
    private String fontFamily = "Arial, sans-serif";
    
    @Column(name = "font_size_base")
    @Builder.Default
    private Integer fontSizeBase = 14;
    
    @Column(name = "font_size_large")
    @Builder.Default
    private Integer fontSizeLarge = 18;
    
    @Column(name = "font_size_small")
    @Builder.Default
    private Integer fontSizeSmall = 12;
    
    // Layout
    @Column(name = "border_radius")
    @Builder.Default
    private Integer borderRadius = 4;
    
    @Column(name = "box_shadow")
    @Builder.Default
    private String boxShadow = "0 0.125rem 0.25rem rgba(0, 0, 0, 0.075)";
    
    @Column(name = "animation_duration")
    @Builder.Default
    private Integer animationDuration = 300;
    
    // Logo settings
    @Column(name = "show_logo")
    @Builder.Default
    private Boolean showLogo = true;
    
    @Column(name = "logo_width")
    @Builder.Default
    private Integer logoWidth = 150;
    
    @Column(name = "logo_height")
    @Builder.Default
    private Integer logoHeight = 50;
    
    // Additional settings
    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

