package com.chich.maqoor.service.impl;

import com.chich.maqoor.entity.User;
import com.chich.maqoor.repository.GarmentReturnRepository;
import com.chich.maqoor.repository.GarmentScanRepository;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.entity.constant.UserState;
import com.chich.maqoor.repository.UserRepository;
import com.chich.maqoor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

//this

import java.util.List;
import java.util.Optional;


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
}
