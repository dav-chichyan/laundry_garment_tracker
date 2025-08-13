package com.chich.maqoor.entity;

import com.chich.maqoor.entity.constant.OrderState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Entity
@Table(name = "orders")
@Data
public class Orders {

    @Id
    @Column(name = "order_id")
    private int orderId;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    private List<Garments> garments;

    @Column(name = "type")
    private String type;

    @Column(name = "express")
    private boolean express;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private OrderState orderState;


}
