package com.chich.maqoor.entity;

import com.chich.maqoor.entity.constant.Departments;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "user_departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDepartment implements Serializable {
    
    @EmbeddedId
    private UserDepartmentId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    @JoinColumn(name = "department_id")
    private Departments department;
    
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDepartmentId implements Serializable {
        private Integer userId;
        private Departments departmentId;
    }
}
