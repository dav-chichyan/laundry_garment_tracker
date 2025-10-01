package com.chich.maqoor.controller;

import com.chich.maqoor.service.DesignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/theme")
@RequiredArgsConstructor
@Slf4j
public class ThemeController {
    
    private final DesignService designService;
    
    @GetMapping(value = "/css", produces = "text/css")
    public ResponseEntity<String> getActiveThemeCss() {
        try {
            String css = designService.getActiveThemeCss();
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/css"))
                    .body(css);
        } catch (Exception e) {
            log.error("Error getting active theme CSS: {}", e.getMessage(), e);
            // Return minimal CSS as fallback
            String fallbackCss = ":root { --primary-color: #007bff; --secondary-color: #6c757d; }";
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/css"))
                    .body(fallbackCss);
        }
    }
}

