package com.chich.maqoor.rest;


import com.chich.maqoor.entity.GarmentScan;
import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.repository.GarmentRepository;
import com.chich.maqoor.repository.GarmentScanRepository;
import com.chich.maqoor.repository.UserRepository;
import com.chich.maqoor.service.GarmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class BarcodeScannerController {

    @Autowired
    private GarmentRepository garmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GarmentScanRepository garmentScanRepository;

    @Autowired
    private GarmentService garmentService;

    @PostMapping("/scan")
    public ResponseEntity<?> scanBarcode(@RequestBody com.chich.maqoor.dto.ScanRequestDto request) {
        try {
            // Validate that the garment exists in the database by cleanCloudGarmentId
            Garments garment = garmentRepository.findByCleanCloudGarmentId(String.valueOf(request.getGarmentId()));
            if (garment == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Garment ID " + request.getGarmentId() + " not found in database");
                errorResponse.put("errorCode", "GARMENT_NOT_FOUND");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            User user = userRepository.findById(request.getUserId()).orElseThrow();

            // NEW VALIDATION: Check if garment is already in the requested department
            if (garment.getDepartmentId() == request.getDepartment()) {
                Map<String, Object> sameDepartmentResponse = new HashMap<>();
                sameDepartmentResponse.put("success", false);
                sameDepartmentResponse.put("message", "Garment " + request.getGarmentId() + " is already in " + request.getDepartment() + " department. No need to scan again.");
                sameDepartmentResponse.put("errorCode", "ALREADY_IN_DEPARTMENT");
                sameDepartmentResponse.put("currentDepartment", garment.getDepartmentId().name());
                sameDepartmentResponse.put("lastUpdate", garment.getLastUpdate());
                return ResponseEntity.badRequest().body(sameDepartmentResponse);
            }

            // Check if this garment was already scanned by the same user in the same department recently (within last 5 minutes)
            Date fiveMinutesAgo = new Date(System.currentTimeMillis() - (5 * 60 * 1000));
            List<GarmentScan> recentScans = garmentScanRepository.findByGarment_GarmentIdAndUser_IdAndDepartmentAndScannedAtAfter(
                garment.getGarmentId(), request.getUserId(), request.getDepartment(), fiveMinutesAgo);
            
            if (!recentScans.isEmpty()) {
                Map<String, Object> duplicateResponse = new HashMap<>();
                duplicateResponse.put("success", false);
                duplicateResponse.put("message", "Garment " + request.getGarmentId() + " was already scanned by you in " + request.getDepartment() + " recently");
                duplicateResponse.put("errorCode", "DUPLICATE_SCAN");
                duplicateResponse.put("lastScanTime", recentScans.get(0).getScannedAt());
                return ResponseEntity.badRequest().body(duplicateResponse);
            }

            // Check if this garment was already scanned by the same department recently (within last 10 minutes)
            Date tenMinutesAgo = new Date(System.currentTimeMillis() - (10 * 60 * 1000));
            List<GarmentScan> departmentRecentScans = garmentScanRepository.findByGarment_GarmentIdAndDepartmentAndScannedAtAfter(
                garment.getGarmentId(), request.getDepartment(), tenMinutesAgo);
            
            if (!departmentRecentScans.isEmpty()) {
                Map<String, Object> departmentDuplicateResponse = new HashMap<>();
                departmentDuplicateResponse.put("success", false);
                departmentDuplicateResponse.put("message", "Garment " + request.getGarmentId() + " was already scanned in " + request.getDepartment() + " recently by another staff member");
                departmentDuplicateResponse.put("errorCode", "DEPARTMENT_DUPLICATE_SCAN");
                departmentDuplicateResponse.put("lastScanTime", departmentRecentScans.get(0).getScannedAt());
                departmentDuplicateResponse.put("lastScannedBy", departmentRecentScans.get(0).getUser().getName());
                return ResponseEntity.badRequest().body(departmentDuplicateResponse);
            }

            // Use the new workflow validation method that returns return status
            boolean isReturn = garmentService.updateGarmentDepartmentWithValidationAndReturnStatus(garment.getGarmentId(), request.getDepartment(), request.getUserId());

            GarmentScan scan = new GarmentScan();
            scan.setGarment(garment);
            scan.setUser(user);
            scan.setDepartment(request.getDepartment());
            scan.setScannedAt(new Date());
            garmentScanRepository.save(scan);

            // Return success response with garment details and return status
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", isReturn ? "Garment scanned successfully (workflow return recorded)" : "Garment scanned successfully");
            successResponse.put("isReturn", isReturn);
            successResponse.put("garment", createGarmentResponse(garment));
            successResponse.put("scanId", scan.getId());
            successResponse.put("scannedAt", scan.getScannedAt());
            
            if (isReturn) {
                successResponse.put("returnMessage", "This scan was recorded as a workflow return. Check admin dashboard for details.");
            }

            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error processing scan: " + e.getMessage());
            errorResponse.put("errorCode", "SCAN_ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/garment/{garmentId}/details")
    public ResponseEntity<?> getGarmentDetails(@PathVariable String garmentId) {
        try {
            // Search by cleanCloudGarmentId instead of internal garmentId
            Garments garment = garmentRepository.findByCleanCloudGarmentId(garmentId);
            if (garment == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Garment ID " + garmentId + " not found");
                errorResponse.put("errorCode", "GARMENT_NOT_FOUND");
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("garment", createGarmentResponse(garment));

            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving garment details: " + e.getMessage());
            errorResponse.put("errorCode", "RETRIEVAL_ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private Map<String, Object> createGarmentResponse(Garments garment) {
        Map<String, Object> garmentResponse = new HashMap<>();
        garmentResponse.put("garmentId", garment.getGarmentId());
        garmentResponse.put("cleanCloudGarmentId", garment.getCleanCloudGarmentId());
        garmentResponse.put("description", garment.getDescription());
        garmentResponse.put("type", garment.getType());
        garmentResponse.put("color", garment.getColor());
        garmentResponse.put("size", garment.getSize());
        garmentResponse.put("specialInstructions", garment.getSpecialInstructions());
        garmentResponse.put("departmentId", garment.getDepartmentId());
        garmentResponse.put("lastUpdate", garment.getLastUpdate());
        
        // Include order information if available
        if (garment.getOrder() != null) {
            Map<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("orderId", garment.getOrder().getOrderId());
            orderInfo.put("cleanCloudOrderId", garment.getOrder().getCleanCloudOrderId());
            orderInfo.put("customerName", garment.getOrder().getCustomerName());
            orderInfo.put("customerPhone", garment.getOrder().getCustomerPhone());
            orderInfo.put("orderNumber", garment.getOrder().getOrderNumber());
            orderInfo.put("type", garment.getOrder().getType());
            orderInfo.put("status", garment.getOrder().getStatus());
            orderInfo.put("orderState", garment.getOrder().getOrderState());
            garmentResponse.put("order", orderInfo);
        }

        return garmentResponse;
    }

    @GetMapping("/orders/{orderId}/garments")
    public List<Garments> garmentsByOrder(@PathVariable int orderId) {
        return garmentService.listByOrderId(orderId);
    }

    @GetMapping("/stats/department/{department}")
    public Map<Integer, Long> countByUserInDepartment(
            @PathVariable Departments department,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date to) {
        List<Object[]> rows = garmentScanRepository.countByUserInDepartmentBetween(department, from, to);
        return rows.stream().collect(Collectors.toMap(r -> (Integer) r[0], r -> (Long) r[1]));
    }
}
