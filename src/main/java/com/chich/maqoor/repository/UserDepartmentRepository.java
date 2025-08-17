package com.chich.maqoor.repository;

import com.chich.maqoor.entity.UserDepartment;
import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {
    
    List<UserDepartment> findByUser(User user);
    
    List<UserDepartment> findByDepartment(Departments department);
    
    @Query("SELECT ud FROM UserDepartment ud WHERE ud.user.id = :userId AND ud.department = :department")
    List<UserDepartment> findByUserIdAndDepartment(@Param("userId") int userId, @Param("department") Departments department);
    
    @Query("SELECT COUNT(ud) FROM UserDepartment ud WHERE ud.user.id = :userId")
    long countByUserId(@Param("userId") int userId);
    
    @Query("SELECT COUNT(ud) FROM UserDepartment ud WHERE ud.department = :department")
    long countByDepartment(@Param("department") Departments department);
}
