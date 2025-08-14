package com.chich.maqoor.entity;

import com.chich.maqoor.entity.constant.Departments;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "garments")
public class Garments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "garment_id")
    private int garmentId;

    @Column(name = "cleancloud_garment_id")
    private String cleanCloudGarmentId;

    @ManyToOne
    @JoinColumn(name = "orders_id")
    private Orders order;

    @Column(name = "description")
    private String description;

    @Column(name = "type")
    private String type;

    @Column(name = "color")
    private String color;

    @Column(name = "size")
    private String size;

    @Column(name = "special_instructions")
    private String specialInstructions;

    @Column(name = "department_id")
    @Enumerated(EnumType.STRING)
    private Departments departmentId;

    @Column(name = "last_update")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdate;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        lastUpdate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdate = new Date();
    }
}
