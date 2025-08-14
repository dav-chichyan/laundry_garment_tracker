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

@Controller
@RequestMapping("/department")
public class DepartmentScannerController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/scanner/{department}")
    public String departmentScanner(@PathVariable String department, Model model) {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User currentUser = userService.findByEmail(email).orElse(null);
            if (currentUser == null) {
                return "redirect:/auth/login";
            }
            
            // Validate department access
            Departments userDept = currentUser.getDepartment();
            if (userDept == null || !userDept.name().equalsIgnoreCase(department)) {
                // User doesn't have access to this department
                return "redirect:/department/scanner/" + 
                       (userDept != null ? userDept.name().toLowerCase() : "reception");
            }
            
            // Convert string to enum and validate
            Departments dept = Departments.valueOf(department.toUpperCase());
            model.addAttribute("departmentName", dept.toString());
            model.addAttribute("userName", currentUser.getName());
            model.addAttribute("userDepartment", currentUser.getDepartment().toString());
            
            return "department/scanner";
        } catch (IllegalArgumentException e) {
            // Invalid department
            return "redirect:/admin/users";
        }
    }
    
    @GetMapping("/scanner")
    public String defaultScanner(Model model) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User currentUser = userService.findByEmail(email).orElse(null);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }
        
        String department = currentUser.getDepartment() != null ? 
            currentUser.getDepartment().name().toLowerCase() : "reception";
        
        return "redirect:/department/scanner/" + department;
    }
}
