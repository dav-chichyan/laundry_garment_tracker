package com.chich.maqoor.config;

import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     Authentication authentication) throws IOException, ServletException {
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        
        try {
            User user = userService.findByEmail(email).orElse(null);
            
            if (user != null) {
                if (Role.ADMIN.equals(user.getRole())) {
                    // Admin users go to admin dashboard
                    setDefaultTargetUrl("/admin/users");
                } else {
                    // Regular staff users go to their department scanner
                    String department = user.getDepartment() != null ? 
                        user.getDepartment().name().toLowerCase() : "reception";
                    setDefaultTargetUrl("/department/scanner/" + department);
                }
            } else {
                // Fallback for admin dashboard
                setDefaultTargetUrl("/admin/users");
            }
        } catch (Exception e) {
            // Fallback for admin dashboard on error
            setDefaultTargetUrl("/admin/users");
        }
        
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
