package com.chich.maqoor.entity;

import com.chich.maqoor.entity.constant.Departments;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "garment_returns")
public class GarmentReturn {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "garment_id")
    private Garments garment;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "from_department")
    private Departments fromDepartment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "to_department")
    private Departments toDepartment;
    
    @Column(name = "return_reason")
    private String returnReason;
    
    @Column(name = "garment_type")
    private String garmentType;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "return_time")
    private Date returnTime;
    
    @Column(name = "expected_next_department")
    private String expectedNextDepartment;
}
