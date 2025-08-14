package com.chich.maqoor.service.impl;

import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.GarmentReturn;
import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.repository.GarmentRepository;
import com.chich.maqoor.repository.GarmentReturnRepository;
import com.chich.maqoor.repository.UserRepository;
import com.chich.maqoor.service.GarmentService;
import com.chich.maqoor.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class GarmentServiceImpl implements GarmentService {

    @Autowired
    private GarmentRepository garmentRepository;
    
    @Autowired
    private GarmentReturnRepository garmentReturnRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WorkflowService workflowService;

    @Override
    public java.util.List<Garments> listByOrderId(int orderId) {
        return garmentRepository.findByOrder_OrderId(orderId);
    }

    @Override
    public Garments updateGarmentDepartment(int garmentId, Departments department) {
        Garments g = garmentRepository.findById(garmentId).orElseThrow();
        g.setDepartmentId(department);
        g.setLastUpdate(new Date());
        return garmentRepository.save(g);
    }
    
    @Override
    public Departments getNextDepartment(int garmentId) {
        Garments garment = garmentRepository.findById(garmentId).orElseThrow();
        String garmentType = determineGarmentType(garment);
        return workflowService.getNextDepartment(garment.getDepartmentId(), garmentType);
    }
    
    @Override
    public Garments updateGarmentDepartmentWithValidation(int garmentId, Departments newDepartment, int userId) {
        Garments garment = garmentRepository.findById(garmentId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        Departments currentDepartment = garment.getDepartmentId();
        String garmentType = determineGarmentType(garment);
        
        // Check if this is a valid transition
        if (!workflowService.isValidTransition(currentDepartment, newDepartment, garmentType)) {
            // Record this as a return/flow violation
            recordReturn(garment, user, currentDepartment, newDepartment, garmentType);
        }
        
        // Update the garment department
        garment.setDepartmentId(newDepartment);
        garment.setLastUpdate(new Date());
        return garmentRepository.save(garment);
    }
    
    @Override
    public boolean isValidDepartmentTransition(int garmentId, Departments newDepartment) {
        Garments garment = garmentRepository.findById(garmentId).orElseThrow();
        String garmentType = determineGarmentType(garment);
        return workflowService.isValidTransition(garment.getDepartmentId(), newDepartment, garmentType);
    }
    
    /**
     * Record a return/flow violation
     */
    private void recordReturn(Garments garment, User user, Departments fromDepartment, 
                            Departments toDepartment, String garmentType) {
        GarmentReturn returnRecord = new GarmentReturn();
        returnRecord.setGarment(garment);
        returnRecord.setUser(user);
        returnRecord.setFromDepartment(fromDepartment);
        returnRecord.setToDepartment(toDepartment);
        returnRecord.setGarmentType(garmentType);
        returnRecord.setReturnTime(new Date());
        returnRecord.setReturnReason("Invalid department transition");
        
        // Get the expected next department
        Departments expectedNext = workflowService.getNextDepartment(fromDepartment, garmentType);
        returnRecord.setExpectedNextDepartment(expectedNext != null ? expectedNext.name() : "Unknown");
        
        garmentReturnRepository.save(returnRecord);
    }
    
    /**
     * Determine garment type based on garment properties
     */
    private String determineGarmentType(Garments garment) {
        // This is a simple implementation - you might want to enhance this
        if (garment.getType() != null) {
            return garment.getType().toUpperCase();
        }
        
        // Default to CLOTHES if no type specified
        return "CLOTHES";
    }
}
