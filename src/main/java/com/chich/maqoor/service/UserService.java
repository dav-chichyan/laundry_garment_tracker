package com.chich.maqoor.service;

import com.chich.maqoor.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    List<User> findAll();
    void save(User user);

    User findById(int userId);
    User findByEmail(String email);
}
