package com.chich.maqoor.entity;

import com.chich.maqoor.entity.constant.Departments;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_departments")
@Data
public class UserDepartment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private Departments department;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
