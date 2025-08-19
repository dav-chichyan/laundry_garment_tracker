package com.chich.maqoor.dto;

import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.entity.constant.UserState;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UserUpdateRequestDto {
    
    @NotNull(message = "User ID is required")
    private Integer id;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotNull(message = "Department is required")
    private Departments department;
    
    @NotNull(message = "Role is required")
    private Role role;
    
    @NotNull(message = "User State is required")
    private UserState state;
    
    // Constructors
    public UserUpdateRequestDto() {}
    
    public UserUpdateRequestDto(Integer id, String name, String email, Departments department, 
                               Role role, UserState state) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.role = role;
        this.state = state;
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
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
    
    public UserState getState() {
        return state;
    }
    
    public void setState(UserState state) {
        this.state = state;
    }
    

}
