package com.chich.maqoor.service.impl;

import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.repository.GarmentRepository;
import com.chich.maqoor.service.GarmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GarmentServiceImpl implements GarmentService {

    @Autowired
    private GarmentRepository garmentRepository;


    @Override
    public java.util.List<Garments> listByOrderId(int orderId) {
        return garmentRepository.findByOrder_OrderId(orderId);
    }

    @Override
    public Garments updateGarmentDepartment(int garmentId, Departments department) {
        Garments g = garmentRepository.findById(garmentId).orElseThrow();
        g.setDepartmentId(department);
        g.setLastUpdate(new java.util.Date());
        return garmentRepository.save(g);
    }
}
