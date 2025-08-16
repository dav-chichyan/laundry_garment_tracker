package com.chich.maqoor.controller.admin;

import com.chich.maqoor.dto.PasswordResetRequestDto;
import com.chich.maqoor.dto.UserCreateRequestDto;
import com.chich.maqoor.dto.UserUpdateRequestDto;
import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class UserManagementController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users-management")
    public String userManagementPage(
            Model model,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "department", required = false) Departments departmentFilter) {

        List<User> users = userService.findAll();

        // Filter by search query (name, email, username)
        if (query != null && !query.trim().isEmpty()) {
            final String q = query.trim().toLowerCase();
            users = users.stream()
                    .filter(u ->
                            (u.getName() != null && u.getName().toLowerCase().contains(q)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)) ||
                            (u.getUsername() != null && u.getUsername().toLowerCase().contains(q)))
                    .toList();
        }

        // Filter by department if provided (admins typically have null department)
        if (departmentFilter != null) {
            final Departments dept = departmentFilter;
            users = users.stream()
                    .filter(u -> u.getDepartment() == dept)
                    .toList();
        }

        // Sort: Admins first, then by name (case-insensitive)
        users = users.stream()
                .sorted((a, b) -> {
                    boolean aIsAdmin = a.getRole() == Role.ADMIN;
                    boolean bIsAdmin = b.getRole() == Role.ADMIN;
                    if (aIsAdmin && !bIsAdmin) return -1;
                    if (!aIsAdmin && bIsAdmin) return 1;
                    String an = a.getName() != null ? a.getName() : "";
                    String bn = b.getName() != null ? b.getName() : "";
                    return an.compareToIgnoreCase(bn);
                })
                .toList();

        model.addAttribute("users", users);
        model.addAttribute("departments", Departments.values());
        model.addAttribute("roles", Role.values());
        model.addAttribute("q", query);
        model.addAttribute("departmentFilter", departmentFilter);
        return "auth/admin/user-management";
    }

    @PostMapping("/users/create")
    public String createUser(@ModelAttribute UserCreateRequestDto requestDto,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation errors occurred");
            return "redirect:/admin/users-management";
        }

        if (!requestDto.isPasswordMatching()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match");
            return "redirect:/admin/users-management";
        }

        try {
            // Check if email already exists
            if (userService.findByEmail(requestDto.getEmail()).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email already exists");
                return "redirect:/admin/users-management";
            }

            User user = new User();
            user.setName(requestDto.getName());
            user.setEmail(requestDto.getEmail());
            user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
            
            // Set department based on role
            if (requestDto.getRole() == Role.ADMIN) {
                // Admin users don't need a specific department
                user.setDepartment(null);
            } else {
                user.setDepartment(requestDto.getDepartment());
            }
            
            user.setRole(requestDto.getRole());
            user.setStatus(com.chich.maqoor.entity.constant.UserStatus.ACTIVE);
            
            // Handle schedule times from form
            List<String> scheduleTimes = requestDto.getScheduleTimes();
            if (scheduleTimes != null && !scheduleTimes.isEmpty()) {
                scheduleTimes.removeIf(time -> time == null || time.trim().isEmpty());
            }
            user.setScheduleTimes(scheduleTimes);

            userService.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating user: " + e.getMessage());
        }

        return "redirect:/admin/users-management";
    }

    @PostMapping("/users/update")
    public String updateUser(@ModelAttribute UserUpdateRequestDto requestDto,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation errors occurred");
            return "redirect:/admin/users-management";
        }

        try {
            User existingUser = userService.findById(requestDto.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if email is being changed and if it already exists
            if (!existingUser.getEmail().equals(requestDto.getEmail()) &&
                userService.findByEmail(requestDto.getEmail()).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email already exists");
                return "redirect:/admin/users-management";
            }

            existingUser.setName(requestDto.getName());
            existingUser.setEmail(requestDto.getEmail());
            
            // Set department based on role
            if (requestDto.getRole() == Role.ADMIN) {
                // Admin users don't need a specific department
                existingUser.setDepartment(null);
            } else {
                existingUser.setDepartment(requestDto.getDepartment());
            }
            
            existingUser.setRole(requestDto.getRole());
            
            // Handle schedule times from form
            List<String> scheduleTimes = requestDto.getScheduleTimes();
            if (scheduleTimes != null && !scheduleTimes.isEmpty()) {
                scheduleTimes.removeIf(time -> time == null || time.trim().isEmpty());
            }
            existingUser.setScheduleTimes(scheduleTimes);

            userService.save(existingUser);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user: " + e.getMessage());
        }

        return "redirect:/admin/users-management";
    }

    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam("id") Integer userId,
                           RedirectAttributes redirectAttributes) {
        
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Prevent admin from deleting themselves
            if (user.getRole() == Role.ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete admin users");
                return "redirect:/admin/users-management";
            }

            // Remove dependent records first to avoid FK violations
            userService.deleteUserAndAssociations(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting user: " + e.getMessage());
        }

        return "redirect:/admin/users-management";
    }

    @PostMapping("/users/reset-password")
    public String resetPassword(@ModelAttribute PasswordResetRequestDto requestDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation errors occurred");
            return "redirect:/admin/users-management";
        }

        if (!requestDto.isPasswordMatching()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match");
            return "redirect:/admin/users-management";
        }

        try {
            User user = userService.findById(requestDto.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setPassword(passwordEncoder.encode(requestDto.getNewPassword()));
            userService.save(user);
            
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error resetting password: " + e.getMessage());
        }

        return "redirect:/admin/users-management";
    }
}
