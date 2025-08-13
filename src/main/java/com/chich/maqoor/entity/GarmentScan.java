package com.chich.maqoor.entity;

import com.chich.maqoor.entity.constant.Departments;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "garment_scans")
public class GarmentScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "garment_id")
    private Garments garment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private Departments department;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "scanned_at", nullable = false)
    private Date scannedAt;
}


