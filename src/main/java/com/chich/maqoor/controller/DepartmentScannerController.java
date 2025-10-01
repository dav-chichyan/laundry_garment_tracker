package com.chich.maqoor.controller;

import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.service.UserService;
import com.chich.maqoor.repository.UserDepartmentRepository;
import java.util.Set;
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
    
    @Autowired
    private UserDepartmentRepository userDepartmentRepository;
    
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
            
            // Validate department access using the new UserDepartment relationship
            Set<Departments> userDepartments = userDepartmentRepository.findDepartmentsByUserId(currentUser.getId());
            boolean hasAccess = userDepartments.stream()
                    .anyMatch(dept -> dept.name().equalsIgnoreCase(department));
            
            if (!hasAccess) {
                log.warn("User {} does not have access to department {}. User's departments: {}", 
                        currentUser.getName(), department, userDepartments);
                // User doesn't have access to this department, redirect to department selection
                if (!userDepartments.isEmpty()) {
                    if (userDepartments.size() > 1) {
                        // Multiple departments - go to selection page
                        return "redirect:/department/select";
                    } else {
                        // Single department - redirect to that department
                        String firstDept = userDepartments.iterator().next().name().toLowerCase();
                        return "redirect:/department/scanner/" + firstDept;
                    }
                } else {
                    // No departments assigned, redirect to login or show error
                    return "redirect:/auth/login?error=no_department";
                }
            }
            
            // Convert string to enum and validate
            Departments dept = Departments.valueOf(department.toUpperCase());
            model.addAttribute("departmentName", dept.toString());
            model.addAttribute("userId", currentUser.getId());
            model.addAttribute("userName", currentUser.getName());
            
            // Set the userDepartment to the department they're actually working in
            model.addAttribute("userDepartment", dept.toString());
            
            log.info("Adding to model - userId: {}, userName: {}, userDepartment: {}", 
                    currentUser.getId(), currentUser.getName(), dept.toString());
            
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
        
        // Get user's departments from the new relationship
        Set<Departments> userDepartments = userDepartmentRepository.findDepartmentsByUserId(currentUser.getId());
        
        String department;
        if (!userDepartments.isEmpty()) {
            department = userDepartments.iterator().next().name().toLowerCase();
        } else {
            // No departments assigned, redirect to login with error
            return "redirect:/auth/login?error=no_department";
        }
        
        log.info("Redirecting to department scanner: {}", department);
        
        return "redirect:/department/scanner/" + department;
    }
    
    @GetMapping("/select")
    public String departmentSelection(Model model) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        log.info("Department selection page accessed, user email: {}", email);
        
        User currentUser = userService.findByEmail(email).orElse(null);
        if (currentUser == null) {
            log.warn("User not found for email: {}", email);
            return "redirect:/auth/login";
        }
        
        // Get user's departments
        Set<Departments> userDepartments = userDepartmentRepository.findDepartmentsByUserId(currentUser.getId());
        
        if (userDepartments.isEmpty()) {
            log.warn("User {} has no departments assigned", currentUser.getName());
            return "redirect:/auth/login?error=no_department";
        }
        
        if (userDepartments.size() == 1) {
            // Only one department, redirect directly to scanner
            String department = userDepartments.iterator().next().name().toLowerCase();
            return "redirect:/department/scanner/" + department;
        }
        
        // Multiple departments - show selection page
        model.addAttribute("userName", currentUser.getName());
        model.addAttribute("departments", userDepartments);
        
        log.info("Showing department selection for user {} with {} departments", 
                currentUser.getName(), userDepartments.size());
        
        return "department/select-department";
    }
}
