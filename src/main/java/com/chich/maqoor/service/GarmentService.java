package com.chich.maqoor.service;

import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.constant.Departments;
import org.springframework.stereotype.Service;

@Service
public interface GarmentService {

    java.util.List<Garments> listByOrderId(int orderId);

    Garments updateGarmentDepartment(int garmentId, Departments department);
}
