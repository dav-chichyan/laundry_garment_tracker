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
    @Column(name = "garment_id")
    private int garmentId;

    @ManyToOne
    @JoinColumn(name = "orders_id")
    private Orders order;

    @Column(name = "description")
    private String description;

    @Column(name = "department_id")
    @Enumerated(EnumType.STRING)
    private Departments departmentId;

    @Column(name = "last_update")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdate;

}
