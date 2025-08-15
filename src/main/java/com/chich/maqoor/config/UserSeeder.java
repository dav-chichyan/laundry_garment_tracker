package com.chich.maqoor.config;

import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.entity.constant.UserState;
import com.chich.maqoor.entity.constant.UserStatus;
import com.chich.maqoor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("UserSeeder starting...");
        log.info("Current user count: {}", userRepository.count());
        
        // Only run if no users exist (to avoid duplicates)
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping user seeding");
            return;
        }

        log.info("Starting user seeding for all departments...");

        try {
            // Create users for all departments
            createUser("Admin User", "admin@maqoor.com", "admin123", Role.ADMIN, Departments.RECEPTION);
            createUser("John Reception", "john@maqoor.com", "john123", Role.USER, Departments.RECEPTION);
            createUser("Sarah Examination", "sarah@maqoor.com", "sarah123", Role.USER, Departments.EXAMINATION);
            createUser("Mike Washing", "mike@maqoor.com", "mike123", Role.USER, Departments.WASHING);
            createUser("Lisa Stain Removal", "lisa@maqoor.com", "lisa123", Role.USER, Departments.STAIN_REMOVAL);
            createUser("David Dry Cleaning", "david@maqoor.com", "david123", Role.USER, Departments.DRY_CLEANING);
            createUser("Emma Shoes", "emma@maqoor.com", "emma123", Role.USER, Departments.SHOES);
            createUser("Tom Ironing", "tom@maqoor.com", "tom123", Role.USER, Departments.IRONING);
            createUser("Anna Packaging", "anna@maqoor.com", "anna123", Role.USER, Departments.PACKAGING);
            createUser("Chris Delivery", "chris@maqoor.com", "chris123", Role.USER, Departments.DELIVERY);
            createUser("Maria Locker", "maria@maqoor.com", "maria123", Role.USER, Departments.LOCKER);
            createUser("Alex Alteration", "alex@maqoor.com", "alex123", Role.USER, Departments.ALTERATION);
            createUser("Sophie Finishing", "sophie@maqoor.com", "sophie123", Role.USER, Departments.FINISHING);

            log.info("User seeding completed successfully. Created {} users.", userRepository.count());
            
        } catch (Exception e) {
            log.error("Error during user seeding: {}", e.getMessage(), e);
        }
    }

    private void createUser(String name, String email, String password, Role role, Departments department) {
        try {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setState(UserState.ACTIVE);
            user.setStatus(UserStatus.ACTIVE);
            user.setUsername(email);
            user.setDepartment(department);
            
            User savedUser = userRepository.save(user);
            log.info("Created user: {} ({}) in department: {}", savedUser.getName(), savedUser.getEmail(), savedUser.getDepartment());
            
        } catch (Exception e) {
            log.error("Error creating user {}: {}", email, e.getMessage());
        }
    }
}
