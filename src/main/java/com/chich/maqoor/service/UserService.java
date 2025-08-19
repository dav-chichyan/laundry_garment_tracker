package com.chich.maqoor.service;

import com.chich.maqoor.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface UserService {
    List<User> findAll();
    void save(User user);
    void deleteById(int userId);
    void deleteUserAndAssociations(int userId);

    Optional<User> findById(int userId);
    Optional<User> findByEmail(String email);
}
