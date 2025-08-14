package com.chich.maqoor.service.impl;

import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.service.WorkflowService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    // Define the workflow rules for different garment types
    private static final Map<String, List<Departments>> WORKFLOW_RULES = new HashMap<>();
    
    static {
        // Regular clothes flow
        WORKFLOW_RULES.put("CLOTHES", Arrays.asList(
            Departments.RECEPTION,
            Departments.EXAMINATION,
            Departments.WASHING,
            Departments.STAIN_REMOVAL,
            Departments.WASHING, // Can go back to washing after stain removal
            Departments.IRONING,
            Departments.PACKAGING,
            Departments.DELIVERY
        ));
        
        // Shoes flow
        WORKFLOW_RULES.put("SHOES", Arrays.asList(
            Departments.RECEPTION,
            Departments.EXAMINATION,
            Departments.SHOES,
            Departments.PACKAGING,
            Departments.DELIVERY
        ));
        
        // Dry cleaning flow
        WORKFLOW_RULES.put("DRY_CLEAN", Arrays.asList(
            Departments.RECEPTION,
            Departments.EXAMINATION,
            Departments.DRY_CLEANING,
            Departments.STAIN_REMOVAL,
            Departments.DRY_CLEANING, // Can go back to dry cleaning after stain removal
            Departments.IRONING,
            Departments.PACKAGING,
            Departments.DELIVERY
        ));
        
        // Ironing only flow
        WORKFLOW_RULES.put("IRONING_ONLY", Arrays.asList(
            Departments.RECEPTION,
            Departments.EXAMINATION,
            Departments.IRONING,
            Departments.PACKAGING,
            Departments.DELIVERY
        ));
    }

    @Override
    public Departments getNextDepartment(Departments currentDepartment, String garmentType) {
        List<Departments> flow = getExpectedFlow(garmentType);
        int currentIndex = flow.indexOf(currentDepartment);
        
        if (currentIndex == -1 || currentIndex >= flow.size() - 1) {
            return null; // No next department or invalid current department
        }
        
        return flow.get(currentIndex + 1);
    }

    @Override
    public boolean isValidTransition(Departments fromDepartment, Departments toDepartment, String garmentType) {
        List<Departments> flow = getExpectedFlow(garmentType);
        int fromIndex = flow.indexOf(fromDepartment);
        int toIndex = flow.indexOf(toDepartment);
        
        if (fromIndex == -1 || toIndex == -1) {
            return false; // Invalid departments
        }
        
        // Allow forward movement and some backward movement (like stain removal back to washing)
        return toIndex >= fromIndex || isAllowedBackwardTransition(fromDepartment, toDepartment, garmentType);
    }

    @Override
    public boolean isReturnTransition(Departments fromDepartment, Departments toDepartment, String garmentType) {
        List<Departments> flow = getExpectedFlow(garmentType);
        int fromIndex = flow.indexOf(fromDepartment);
        int toIndex = flow.indexOf(toDepartment);
        
        // A return is when we go backwards in the flow (excluding allowed backward transitions)
        if (fromIndex == -1 || toIndex == -1) {
            return false;
        }
        
        return toIndex < fromIndex && !isAllowedBackwardTransition(fromDepartment, toDepartment, garmentType);
    }

    @Override
    public List<Departments> getExpectedFlow(String garmentType) {
        return WORKFLOW_RULES.getOrDefault(garmentType, WORKFLOW_RULES.get("CLOTHES"));
    }
    
    private boolean isAllowedBackwardTransition(Departments fromDepartment, Departments toDepartment, String garmentType) {
        // Allow stain removal to go back to washing/dry cleaning
        if (fromDepartment == Departments.STAIN_REMOVAL) {
            return toDepartment == Departments.WASHING || toDepartment == Departments.DRY_CLEANING;
        }
        
        // Allow some other specific backward transitions if needed
        return false;
    }
}
