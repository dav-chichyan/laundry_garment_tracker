package com.chich.maqoor.repository;

import com.chich.maqoor.entity.UserDepartment;
import com.chich.maqoor.entity.constant.Departments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, UserDepartment.UserDepartmentId> {
    
    // Find all departments for a specific user
    @Query("SELECT ud.department FROM UserDepartment ud WHERE ud.user.id = :userId")
    Set<Departments> findDepartmentsByUserId(@Param("userId") Integer userId);
    
    // Find all user-department mappings for a specific user
    List<UserDepartment> findByUserId(Integer userId);
    
    // Find all users for a specific department
    @Query("SELECT ud.user.id FROM UserDepartment ud WHERE ud.department = :department")
    List<Integer> findUserIdsByDepartment(@Param("department") Departments department);
    
    // Delete all department assignments for a specific user
    @Modifying
    @Query("DELETE FROM UserDepartment ud WHERE ud.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
    
    // Check if a user has a specific department
    @Query("SELECT COUNT(ud) > 0 FROM UserDepartment ud WHERE ud.user.id = :userId AND ud.department = :department")
    boolean existsByUserIdAndDepartment(@Param("userId") Integer userId, @Param("department") Departments department);
}
