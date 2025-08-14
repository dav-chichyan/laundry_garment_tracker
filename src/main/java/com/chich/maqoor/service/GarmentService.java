package com.chich.maqoor.service;

import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.constant.Departments;
import org.springframework.stereotype.Service;

@Service
public interface GarmentService {

    java.util.List<Garments> listByOrderId(int orderId);

    Garments updateGarmentDepartment(int garmentId, Departments department);
    
    /**
     * Get next department for a garment
     */
    Departments getNextDepartment(int garmentId);
    
    /**
     * Update garment department with workflow validation
     */
    Garments updateGarmentDepartmentWithValidation(int garmentId, Departments newDepartment, int userId);
    
    /**
     * Check if department transition is valid
     */
    boolean isValidDepartmentTransition(int garmentId, Departments newDepartment);
}
