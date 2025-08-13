package com.chich.maqoor.repository;

import com.chich.maqoor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    java.util.Optional<User> findByEmail(String email);
}
