package com.chich.maqoor.service;

import com.chich.maqoor.entity.Garments;

import java.util.List;

public interface OrderService {
    List<Garments> garmentsForOrder(int orderId);
}
