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
import java.util.List;
import java.util.Map;
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
        Garments garment = garmentRepository.findById(request.getGarmentId()).orElseThrow();
        User user = userRepository.findById(request.getUserId()).orElseThrow();

        // Use the new workflow validation method
        garmentService.updateGarmentDepartmentWithValidation(request.getGarmentId(), request.getDepartment(), request.getUserId());

        GarmentScan scan = new GarmentScan();
        scan.setGarment(garment);
        scan.setUser(user);
        scan.setDepartment(request.getDepartment());
        scan.setScannedAt(new Date());
        garmentScanRepository.save(scan);

        return ResponseEntity.ok().build();
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
