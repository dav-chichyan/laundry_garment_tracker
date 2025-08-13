package com.chich.maqoor.service.impl;

import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.repository.GarmentRepository;
import com.chich.maqoor.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private GarmentRepository garmentRepository;

    @Override
    public List<Garments> garmentsForOrder(int orderId) {
        return garmentRepository.findByOrder_OrderId(orderId);
    }
}
