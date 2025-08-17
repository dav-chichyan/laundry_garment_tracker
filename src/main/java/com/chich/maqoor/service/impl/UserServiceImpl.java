package com.chich.maqoor.service.impl;

import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.UserDepartment;
import com.chich.maqoor.repository.GarmentReturnRepository;
import com.chich.maqoor.repository.GarmentScanRepository;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.entity.constant.UserState;
import com.chich.maqoor.repository.UserRepository;
import com.chich.maqoor.repository.UserDepartmentRepository;
import com.chich.maqoor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class UserServiceImpl implements UserService {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GarmentScanRepository garmentScanRepository;

    @Autowired
    private GarmentReturnRepository garmentReturnRepository;

    @Autowired
    private UserDepartmentRepository userDepartmentRepository;

    @Override
    public List<User> findAll() {
       return userRepository.findAll();
    }

    @Override
    public void save(User user) {
        // Only set default values if they're not already set
        if (user.getState() == null) {
            user.setState(UserState.ACTIVE);
        }
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
    }

    @Override
    public Optional<User> findById(int userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void deleteById(int userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public void deleteUserAndAssociations(int userId) {
        // delete dependent rows first to satisfy FK constraints
        garmentScanRepository.deleteByUser_Id(userId);
        garmentReturnRepository.deleteByUser_Id(userId);
        userRepository.deleteById(userId);
    }

    // Enhanced methods for multiple departments
    @Override
    public void addUserDepartment(User user, String department) {
        try {
            Departments dept = Departments.valueOf(department);
            UserDepartment userDept = new UserDepartment();
            userDept.setUser(user);
            userDept.setDepartment(dept);
            userDept.setActive(true);
            userDepartmentRepository.save(userDept);
        } catch (IllegalArgumentException e) {
            // Invalid department name
            throw new RuntimeException("Invalid department: " + department);
        }
    }

    @Override
    public void removeUserDepartment(User user, String department) {
        try {
            Departments dept = Departments.valueOf(department);
            List<UserDepartment> userDepts = userDepartmentRepository.findByUserIdAndDepartment(user.getId(), dept);
            userDepartmentRepository.deleteAll(userDepts);
        } catch (IllegalArgumentException e) {
            // Invalid department name
            throw new RuntimeException("Invalid department: " + department);
        }
    }

    @Override
    public List<String> getUserDepartments(User user) {
        List<UserDepartment> userDepts = userDepartmentRepository.findByUser(user);
        return userDepts.stream()
                .filter(UserDepartment::isActive)
                .map(ud -> ud.getDepartment().name())
                .collect(Collectors.toList());
    }

    @Override
    public void updateUserDepartments(User user, List<String> departments) {
        // Remove existing departments
        List<UserDepartment> existingDepts = userDepartmentRepository.findByUser(user);
        userDepartmentRepository.deleteAll(existingDepts);
        
        // Add new departments
        if (departments != null) {
            for (String dept : departments) {
                addUserDepartment(user, dept);
            }
        }
    }
}
