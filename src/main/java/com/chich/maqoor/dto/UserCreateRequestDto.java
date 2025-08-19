package com.chich.maqoor.dto;

import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserCreateRequestDto {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
    
    private Departments department;
    
    @NotNull(message = "Role is required")
    private Role role;
    
    // Constructors
    public UserCreateRequestDto() {}
    
    public UserCreateRequestDto(String name, String email, String password, String confirmPassword, 
                               Departments department, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.department = department;
        this.role = role;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public Departments getDepartment() {
        return department;
    }
    
    public void setDepartment(Departments department) {
        this.department = department;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    

    
    // Validation methods
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
    
    public boolean isValidDepartmentSelection() {
        System.out.println("DEBUG DTO: Role = " + role);
        System.out.println("DEBUG DTO: Department = " + department);
        System.out.println("DEBUG DTO: Department class = " + (department != null ? department.getClass().getName() : "null"));
        
        // Admin users don't need a department
        if (role == Role.ADMIN) {
            System.out.println("DEBUG DTO: Admin user - validation passed");
            return true;
        }
        // Non-admin users must have a department
        boolean isValid = department != null;
        System.out.println("DEBUG DTO: Non-admin user - validation result: " + isValid);
        return isValid;
    }
}
