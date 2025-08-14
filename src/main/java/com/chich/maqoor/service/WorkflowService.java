package com.chich.maqoor.service;

import com.chich.maqoor.entity.constant.Departments;

public interface WorkflowService {
    
    /**
     * Get the next department for a garment based on current department and garment type
     */
    Departments getNextDepartment(Departments currentDepartment, String garmentType);
    
    /**
     * Validate if the department transition is valid
     */
    boolean isValidTransition(Departments fromDepartment, Departments toDepartment, String garmentType);
    
    /**
     * Check if a transition is a return (going backwards in the flow)
     */
    boolean isReturnTransition(Departments fromDepartment, Departments toDepartment, String garmentType);
    
    /**
     * Get the expected flow for a garment type
     */
    java.util.List<Departments> getExpectedFlow(String garmentType);
}
