package com.chich.maqoor.controller;

import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/department")
public class DepartmentScannerController {
    
    private static final Logger log = LoggerFactory.getLogger(DepartmentScannerController.class);

    @Autowired
    private UserService userService;
    
    @GetMapping("/scanner/{department}")
    public String departmentScanner(@PathVariable String department, Model model) {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            log.info("Department scanner accessed for department: {}, user email: {}", department, email);
            
            User currentUser = userService.findByEmail(email).orElse(null);
            if (currentUser == null) {
                log.warn("User not found for email: {}", email);
                return "redirect:/auth/login";
            }
            
            log.info("Found user: id={}, name={}, department={}", 
                    currentUser.getId(), currentUser.getName(), currentUser.getDepartment());
            
            // Validate department access
            Departments userDept = currentUser.getDepartment();
            if (userDept == null || !userDept.name().equalsIgnoreCase(department)) {
                log.warn("User {} does not have access to department {}. User's department: {}", 
                        currentUser.getName(), department, userDept);
                // User doesn't have access to this department
                return "redirect:/department/scanner/" + 
                       (userDept != null ? userDept.name().toLowerCase() : "reception");
            }
            
            // Convert string to enum and validate
            Departments dept = Departments.valueOf(department.toUpperCase());
            model.addAttribute("departmentName", dept.toString());
            model.addAttribute("userId", currentUser.getId());
            model.addAttribute("userName", currentUser.getName());
            model.addAttribute("userDepartment", currentUser.getDepartment().toString());
            
            log.info("Adding to model - userId: {}, userName: {}, userDepartment: {}", 
                    currentUser.getId(), currentUser.getName(), currentUser.getDepartment());
            
            return "department/scanner";
        } catch (IllegalArgumentException e) {
            log.error("Invalid department: {}", department, e);
            // Invalid department
            return "redirect:/admin/users";
        }
    }
    
    @GetMapping("/scanner")
    public String defaultScanner(Model model) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        log.info("Default scanner accessed, user email: {}", email);
        
        User currentUser = userService.findByEmail(email).orElse(null);
        if (currentUser == null) {
            log.warn("User not found for email: {}", email);
            return "redirect:/auth/login";
        }
        
        log.info("Found user in default scanner: id={}, name={}, department={}", 
                currentUser.getId(), currentUser.getName(), currentUser.getDepartment());
        
        String department = currentUser.getDepartment() != null ? 
            currentUser.getDepartment().name().toLowerCase() : "reception";
        
        log.info("Redirecting to department scanner: {}", department);
        
        return "redirect:/department/scanner/" + department;
    }
}
