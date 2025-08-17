package com.chich.maqoor.entity;

import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.ProblemStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "problems")
@Getter
@Setter
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Link to order record
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders order; // Internal Orders entity

    // The department where problem originated
    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private Departments department;

    // The staff member who reported or is responsible
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProblemStatus status = ProblemStatus.OPEN;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt = new Date();
}


