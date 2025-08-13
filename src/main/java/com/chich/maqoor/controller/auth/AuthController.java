package com.chich.maqoor.controller.auth;

import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("departments", getDepartmentOptions());
        model.addAttribute("user", new User());
        return "auth/registration";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam("name") String name,
                             @RequestParam("email") String email,
                             @RequestParam("password") String password,
                             @RequestParam("department") String department,
                             Model model) {
        try {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setUsername(email); // Use email as username for now
            
            // Handle department and role assignment
            if ("ADMIN".equals(department)) {
                user.setRole(Role.ADMIN);
                user.setDepartment(Departments.RECEPTION); // Default department for admins
            } else {
                user.setRole(Role.USER);
                user.setDepartment(Departments.valueOf(department));
            }
            
            userService.save(user);
            model.addAttribute("success", "User registered successfully! Please login.");
            return "auth/login";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            model.addAttribute("departments", getDepartmentOptions());
            return "auth/registration";
        }
    }

    private java.util.List<java.util.Map<String, String>> getDepartmentOptions() {
        java.util.List<java.util.Map<String, String>> options = new java.util.ArrayList<>();
        
        // Add ADMIN option
        java.util.Map<String, String> adminOption = new java.util.HashMap<>();
        adminOption.put("value", "ADMIN");
        adminOption.put("label", "ADMIN (System Administrator)");
        options.add(adminOption);
        
        // Add regular departments
        for (Departments dept : Departments.values()) {
            java.util.Map<String, String> deptOption = new java.util.HashMap<>();
            deptOption.put("value", dept.name());
            deptOption.put("label", dept.name());
            options.add(deptOption);
        }
        
        return options;
    }
}
