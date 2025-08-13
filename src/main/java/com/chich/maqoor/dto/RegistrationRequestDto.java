package com.chich.maqoor.dto;

import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.entity.constant.UserState;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;


public class RegistrationRequestDto {

    @NotNull(message = "Name is required")
    @Size(min = 2, max = 50)
    private String name;

    @NotBlank(message = "Username is required")
    @Email
    private String email;

    @NotBlank(message = "Password is required")
    private String password;


    public RegistrationRequestDto() {}

    public @NotNull(message = "Name is required") @Size(min = 2, max = 50) String getName() {
        return name;
    }

    public void setName(@NotNull(message = "Name is required") @Size(min = 2, max = 50) String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail( String email) {
        this.email = email;
    }

    public  String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }





    @Override
    public String toString() {
        return "RegistrationRequestDto{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}


