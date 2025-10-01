package com.chich.maqoor.controller.admin;

import com.chich.maqoor.entity.DesignSettings;
import com.chich.maqoor.service.DesignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/design")
@RequiredArgsConstructor
@Slf4j
public class DesignController {
    
    private final DesignService designService;
    
    @GetMapping
    public String designManagement(Model model) {
        List<DesignSettings> themes = designService.getAllThemes();
        DesignSettings activeTheme = designService.getActiveTheme();
        
        model.addAttribute("themes", themes);
        model.addAttribute("activeTheme", activeTheme);
        
        return "auth/admin/design-management";
    }
    
    @GetMapping("/create")
    public String createThemeForm(Model model) {
        model.addAttribute("theme", new DesignSettings());
        model.addAttribute("isEdit", false);
        return "auth/admin/theme-form";
    }
    
    @GetMapping("/edit/{id}")
    public String editThemeForm(@PathVariable Long id, Model model) {
        DesignSettings theme = designService.getThemeById(id);
        model.addAttribute("theme", theme);
        model.addAttribute("isEdit", true);
        return "auth/admin/theme-form";
    }
    
    @PostMapping("/save")
    public String saveTheme(@ModelAttribute DesignSettings theme, 
                           @RequestParam(defaultValue = "false") boolean isEdit,
                           RedirectAttributes redirectAttributes) {
        try {
            if (isEdit) {
                designService.updateTheme(theme.getId(), theme);
                redirectAttributes.addFlashAttribute("successMessage", "Theme updated successfully!");
            } else {
                designService.createTheme(theme);
                redirectAttributes.addFlashAttribute("successMessage", "Theme created successfully!");
            }
        } catch (Exception e) {
            log.error("Error saving theme: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving theme: " + e.getMessage());
            return "redirect:/admin/design/create";
        }
        
        return "redirect:/admin/design";
    }
    
    @PostMapping("/activate/{id}")
    public String activateTheme(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            designService.activateTheme(id);
            redirectAttributes.addFlashAttribute("successMessage", "Theme activated successfully!");
        } catch (Exception e) {
            log.error("Error activating theme: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error activating theme: " + e.getMessage());
        }
        
        return "redirect:/admin/design";
    }
    
    @PostMapping("/duplicate/{id}")
    public String duplicateTheme(@PathVariable Long id, 
                               @RequestParam String newThemeName,
                               RedirectAttributes redirectAttributes) {
        try {
            designService.duplicateTheme(id, newThemeName);
            redirectAttributes.addFlashAttribute("successMessage", "Theme duplicated successfully!");
        } catch (Exception e) {
            log.error("Error duplicating theme: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error duplicating theme: " + e.getMessage());
        }
        
        return "redirect:/admin/design";
    }
    
    @PostMapping("/delete/{id}")
    public String deleteTheme(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            designService.deleteTheme(id);
            redirectAttributes.addFlashAttribute("successMessage", "Theme deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting theme: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting theme: " + e.getMessage());
        }
        
        return "redirect:/admin/design";
    }
    
    @PostMapping("/reset")
    public String resetToDefault(RedirectAttributes redirectAttributes) {
        try {
            designService.resetToDefault();
            redirectAttributes.addFlashAttribute("successMessage", "Reset to default theme successfully!");
        } catch (Exception e) {
            log.error("Error resetting to default: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error resetting to default: " + e.getMessage());
        }
        
        return "redirect:/admin/design";
    }
    
    @GetMapping("/preview/{id}")
    @ResponseBody
    public ResponseEntity<String> previewTheme(@PathVariable Long id) {
        try {
            DesignSettings theme = designService.getThemeById(id);
            String css = generateThemeCss(theme);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(css);
        } catch (Exception e) {
            log.error("Error generating preview: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error generating preview: " + e.getMessage());
        }
    }
    
    @GetMapping("/css")
    @ResponseBody
    public ResponseEntity<String> getActiveThemeCss() {
        try {
            String css = designService.getActiveThemeCss();
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/css"))
                    .body(css);
        } catch (Exception e) {
            log.error("Error getting active theme CSS: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("/* Error loading theme CSS */");
        }
    }
    
    @GetMapping("/demo")
    public String themeDemo() {
        return "demo/theme-demo";
    }
    
    private String generateThemeCss(DesignSettings theme) {
        StringBuilder css = new StringBuilder();
        css.append(":root {\n");
        css.append("  --primary-color: ").append(theme.getPrimaryColor()).append(";\n");
        css.append("  --secondary-color: ").append(theme.getSecondaryColor()).append(";\n");
        css.append("  --success-color: ").append(theme.getSuccessColor()).append(";\n");
        css.append("  --warning-color: ").append(theme.getWarningColor()).append(";\n");
        css.append("  --danger-color: ").append(theme.getDangerColor()).append(";\n");
        css.append("  --info-color: ").append(theme.getInfoColor()).append(";\n");
        css.append("  --light-color: ").append(theme.getLightColor()).append(";\n");
        css.append("  --dark-color: ").append(theme.getDarkColor()).append(";\n");
        css.append("  --header-bg-color: ").append(theme.getHeaderBgColor()).append(";\n");
        css.append("  --header-gradient-color: ").append(theme.getHeaderGradientColor()).append(";\n");
        css.append("  --sidebar-bg-color: ").append(theme.getHeaderGradientColor()).append(";\n");
        css.append("  --sidebar-text-color: ").append(theme.getSidebarTextColor()).append(";\n");
        css.append("  --sidebar-hover-color: ").append(theme.getSidebarHoverColor()).append(";\n");
        css.append("  --card-bg-color: ").append(theme.getCardBgColor()).append(";\n");
        css.append("  --card-header-color: ").append(theme.getCardHeaderColor()).append(";\n");
        css.append("  --font-family: ").append(theme.getFontFamily()).append(";\n");
        css.append("  --font-size-base: ").append(theme.getFontSizeBase()).append("px;\n");
        css.append("  --font-size-large: ").append(theme.getFontSizeLarge()).append("px;\n");
        css.append("  --font-size-small: ").append(theme.getFontSizeSmall()).append("px;\n");
        css.append("  --border-radius: ").append(theme.getBorderRadius()).append("px;\n");
        css.append("  --box-shadow: ").append(theme.getBoxShadow()).append(";\n");
        css.append("  --animation-duration: ").append(theme.getAnimationDuration()).append("ms;\n");
        css.append("}\n");
        
        if (theme.getCustomCss() != null && !theme.getCustomCss().trim().isEmpty()) {
            css.append("\n").append(theme.getCustomCss());
        }
        
        return css.toString();
    }
}
