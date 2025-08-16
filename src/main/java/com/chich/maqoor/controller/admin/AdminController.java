package com.chich.maqoor.controller.admin;


import com.chich.maqoor.dto.RegistrationRequestDto;
import com.chich.maqoor.entity.GarmentScan;
import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.GarmentReturn;
import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.repository.GarmentScanRepository;
import com.chich.maqoor.repository.GarmentRepository;
import com.chich.maqoor.repository.GarmentReturnRepository;
import com.chich.maqoor.service.UserService;
import com.chich.maqoor.service.GarmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import com.chich.maqoor.entity.Orders;
import com.chich.maqoor.repository.OrdersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GarmentScanRepository garmentScanRepository;

    @Autowired
    private GarmentService garmentService;

    @Autowired
    private GarmentRepository garmentRepository;
    
    @Autowired
    private GarmentReturnRepository garmentReturnRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @GetMapping("/users")
    public String usersDashboard(Model model,
                                 @RequestParam(value = "fromDate", required = false) String fromDateStr,
                                 @RequestParam(value = "toDate", required = false) String toDateStr) {
        Date fromDate;
        Date toDate;
        
        if (fromDateStr == null || toDateStr == null) {
            // Default to yesterday to today
            java.time.LocalDateTime startOfDay = java.time.LocalDate.now().minusDays(1).atStartOfDay();
            fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
            toDate = new java.util.Date();
        } else {
            try {
                // Parse the date strings (format: yyyy-MM-ddTHH:mm)
                java.time.LocalDateTime from = java.time.LocalDateTime.parse(fromDateStr);
                java.time.LocalDateTime to = java.time.LocalDateTime.parse(toDateStr);
                fromDate = java.util.Date.from(from.atZone(java.time.ZoneId.systemDefault()).toInstant());
                toDate = java.util.Date.from(to.atZone(java.time.ZoneId.systemDefault()).toInstant());
            } catch (Exception e) {
                // Fallback to default dates if parsing fails
                java.time.LocalDateTime startOfDay = java.time.LocalDate.now().minusDays(1).atStartOfDay();
                fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
                toDate = new java.util.Date();
            }
        }

        List<User> users = userService.findAll();
        
        // Debug logging
        System.out.println("USERS DASHBOARD DEBUG: Total users found: " + users.size());
        users.forEach(user -> System.out.println("USERS DASHBOARD DEBUG: User - ID: " + user.getId() + ", Name: " + user.getName() + ", Role: " + user.getRole() + ", Department: " + user.getDepartment()));
        
        // Get real-time department counts
        Map<Departments, Long> departmentCounts = new HashMap<>();
        for (Departments dept : Departments.values()) {
            long count = garmentRepository.countByDepartmentId(dept);
            departmentCounts.put(dept, count);
        }
        
        // Get user performance data (scans per department)
        Map<Integer, Map<Departments, Long>> userDeptCounts = new HashMap<>();
        for (User user : users) {
            Map<Departments, Long> userCounts = new HashMap<>();
            for (Departments dept : Departments.values()) {
                long count = garmentScanRepository.countByUser_IdAndDepartmentAndScannedAtBetween(
                    user.getId(), dept, fromDate, toDate);
                userCounts.put(dept, count);
            }
            userDeptCounts.put(user.getId(), userCounts);
        }
        
        // Get total scans by user for the date range
        Map<Integer, Long> scansByUser = new HashMap<>();
        for (User user : users) {
            long count = garmentScanRepository.countByUser_IdAndScannedAtBetween(user.getId(), fromDate, toDate);
            scansByUser.put(user.getId(), count);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("departments", Departments.values());
        model.addAttribute("from", fromDate.toString());
        model.addAttribute("to", toDate.toString());
        model.addAttribute("departmentCounts", departmentCounts);
        model.addAttribute("userDeptCounts", userDeptCounts);
        model.addAttribute("scansByUser", scansByUser);
        // Use the new staff performance dashboard template
        return "auth/admin/staff-dashboard";
    }
    
    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable int id,
                             @RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") java.time.LocalDateTime from,
                             @RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") java.time.LocalDateTime to,
                             Model model) {
        Date fromDate;
        Date toDate;
        if (from == null || to == null) {
            java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
            fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
            toDate = new java.util.Date();
        } else {
            fromDate = java.util.Date.from(from.atZone(java.time.ZoneId.systemDefault()).toInstant());
            toDate = java.util.Date.from(to.atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        User user = userService.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        List<GarmentScan> scans = garmentScanRepository.findByUser_IdAndScannedAtBetweenOrderByScannedAtDesc(id, fromDate, toDate);
        
        // Get scan statistics
        Map<Departments, Long> scansByDepartment = new HashMap<>();
        for (Departments dept : Departments.values()) {
            long count = garmentScanRepository.countByUser_IdAndDepartmentAndScannedAtBetween(id, dept, fromDate, toDate);
            scansByDepartment.put(dept, count);
        }
        
        // Get total scans for the period
        long totalScans = garmentScanRepository.countByUser_IdAndScannedAtBetween(id, fromDate, toDate);
        
        model.addAttribute("user", user);
        model.addAttribute("scans", scans);
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
        model.addAttribute("scansByDepartment", scansByDepartment);
        model.addAttribute("totalScans", totalScans);
        return "auth/admin/user-detail";
    }

    @GetMapping("/orders/search")
    public String searchOrder(@RequestParam("orderId") int orderId, Model model) {
        System.out.println("Searching for order: " + orderId);
        try {
            // First, find the order by cleanCloudOrderId
            Optional<Orders> order = ordersRepository.findByCleanCloudOrderId(orderId);
            if (!order.isPresent()) {
                System.out.println("Order with cleanCloudOrderId " + orderId + " not found");
                model.addAttribute("error", "Order not found with ID: " + orderId);
                return "redirect:/admin/users";
            }
            
            // Then get garments for that order
            List<Garments> garments = garmentService.listByOrderId(order.get().getOrderId());
            System.out.println("Found " + garments.size() + " garments for order " + orderId);
            if (garments != null) {
                for (Garments g : garments) {
                    System.out.println("Garment: " + g.getGarmentId() + ", Dept: " + g.getDepartmentId());
                }
            }
            model.addAttribute("orderId", orderId);
            model.addAttribute("order", order.get());
            model.addAttribute("garments", garments);
            System.out.println("Returning template: auth/admin/order-detail");
            return "auth/admin/order-detail";
        } catch (Exception e) {
            System.err.println("Error searching order: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Order not found: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/departments/{department}")
    public String departmentGarments(@PathVariable Departments department, Model model) {
        List<Garments> garments = garmentRepository.findByDepartmentId(department);
        model.addAttribute("department", department);
        model.addAttribute("garments", garments);
        return "auth/admin/department-garments";
    }

    @GetMapping("/test")
    public String test() {
        return "auth/admin/test";
    }

    @GetMapping("/garments/{garmentId}")
    public String garmentHistory(@PathVariable int garmentId, Model model) {
        try {
            Garments garment = garmentRepository.findById(garmentId).orElseThrow();
            List<GarmentScan> scans = garmentScanRepository.findByGarment_GarmentId(garmentId);
            
            model.addAttribute("garment", garment);
            model.addAttribute("scans", scans);
            return "auth/admin/garment-history";
        } catch (Exception e) {
            model.addAttribute("error", "Garment not found: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/returns")
    public String showReturns(@RequestParam(value = "userId", required = false) Integer userId,
                             @RequestParam(value = "fromDate", required = false) String fromDateStr,
                             @RequestParam(value = "toDate", required = false) String toDateStr,
                             Model model) {
        Date fromDate;
        Date toDate;
        
        if (fromDateStr == null || toDateStr == null) {
            // Default to last 7 days
            java.time.LocalDateTime startOfDay = java.time.LocalDate.now().minusDays(7).atStartOfDay();
            fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
            toDate = new java.util.Date();
        } else {
            try {
                java.time.LocalDateTime from = java.time.LocalDateTime.parse(fromDateStr);
                java.time.LocalDateTime to = java.time.LocalDateTime.parse(toDateStr);
                fromDate = java.util.Date.from(from.atZone(java.time.ZoneId.systemDefault()).toInstant());
                toDate = java.util.Date.from(to.atZone(java.time.ZoneId.systemDefault()).toInstant());
            } catch (Exception e) {
                java.time.LocalDateTime startOfDay = java.time.LocalDate.now().minusDays(7).atStartOfDay();
                fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
                toDate = new java.util.Date();
            }
        }
        
        List<GarmentReturn> returns;
        if (userId != null) {
            // Filter by specific user
            returns = garmentReturnRepository.findByUser_IdAndReturnTimeBetween(userId, fromDate, toDate);
            User user = userService.findById(userId).orElse(null);
            model.addAttribute("filteredUser", user);
        } else {
            // Show all returns
            returns = garmentReturnRepository.findByReturnTimeBetween(fromDate, toDate);
        }
        
        model.addAttribute("returns", returns);
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
        // model.addAttribute("departments", Departments.values());
        
        return "auth/admin/returns-detail";
    }

    @GetMapping("/scanned-garments")
    public String showScannedGarments(@RequestParam(value = "userId", required = false) Integer userId,
                                     @RequestParam(value = "department", required = false) Departments department,
                                     @RequestParam(value = "fromDate", required = false) String fromDateStr,
                                     @RequestParam(value = "toDate", required = false) String toDateStr,
                                     Model model) {
        Date fromDate;
        Date toDate;
        
        if (fromDateStr == null || toDateStr == null) {
            // Default to last 7 days
            java.time.LocalDateTime startOfDay = java.time.LocalDate.now().minusDays(7).atStartOfDay();
            fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
            toDate = new java.util.Date();
        } else {
            try {
                java.time.LocalDateTime from = java.time.LocalDateTime.parse(fromDateStr);
                java.time.LocalDateTime to = java.time.LocalDateTime.parse(toDateStr);
                fromDate = java.util.Date.from(from.atZone(java.time.ZoneId.systemDefault()).toInstant());
                toDate = java.util.Date.from(to.atZone(java.time.ZoneId.systemDefault()).toInstant());
            } catch (Exception e) {
                java.time.LocalDateTime startOfDay = java.time.LocalDate.now().minusDays(7).atStartOfDay();
                fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
                toDate = new java.util.Date();
            }
        }
        
        List<GarmentScan> scans;
        if (userId != null && department != null) {
            // Filter by specific user and department
            scans = garmentScanRepository.findByUser_IdAndDepartmentAndScannedAtBetweenOrderByScannedAtDesc(userId, department, fromDate, toDate);
            User user = userService.findById(userId).orElse(null);
            model.addAttribute("filteredUser", user);
            model.addAttribute("filteredDepartment", department);
        } else if (userId != null) {
            // Filter by specific user only
            scans = garmentScanRepository.findByUser_IdAndScannedAtBetweenOrderByScannedAtDesc(userId, fromDate, toDate);
            User user = userService.findById(userId).orElse(null);
            model.addAttribute("filteredUser", user);
        } else if (department != null) {
            // Filter by specific department only
            scans = garmentScanRepository.findByDepartmentAndScannedAtBetweenOrderByScannedAtDesc(department, fromDate, toDate);
            model.addAttribute("filteredDepartment", department);
        } else {
            // Show all scans
            scans = garmentScanRepository.findByScannedAtBetweenOrderByScannedAtDesc(fromDate, toDate);
        }
        
        // Get summary statistics
        Map<Departments, Long> scansByDepartment = new HashMap<>();
        Map<Integer, Long> scansByUser = new HashMap<>();
        
        for (Departments dept : Departments.values()) {
            long count = scans.stream()
                .filter(scan -> scan.getDepartment() == dept)
                .count();
            scansByDepartment.put(dept, count);
        }
        
        for (User user : userService.findAll()) {
            long count = scans.stream()
                .filter(scan -> scan.getUser().getId() == user.getId())
                .count();
            scansByUser.put(user.getId(), count);
        }
        
        model.addAttribute("scans", scans);
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
        // model.addAttribute("departments", Departments.values());
        model.addAttribute("users", userService.findAll());
        // model.addAttribute("scansByDepartment", scansByDepartment);
        // model.addAttribute("scansByUser", scansByUser);
        
        return "auth/admin/scanned-garments";
    }

    @GetMapping("/returns-dashboard")
    public String showReturnsDashboard(Model model,
                                      @RequestParam(value = "fromDate", required = false) String fromDateStr,
                                      @RequestParam(value = "toDate", required = false) String toDateStr) {
        try {
            Date fromDate;
            Date toDate;
            
            if (fromDateStr == null || toDateStr == null) {
                // Default to yesterday to today
                java.time.LocalDateTime startOfDay = java.time.LocalDate.now().minusDays(1).atStartOfDay();
                fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
                toDate = new java.util.Date();
            } else {
                try {
                    // Parse the date strings (format: yyyy-MM-ddTHH:mm)
                    java.time.LocalDateTime from = java.time.LocalDateTime.parse(fromDateStr);
                    java.time.LocalDateTime to = java.time.LocalDateTime.parse(toDateStr);
                    fromDate = java.util.Date.from(from.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    toDate = java.util.Date.from(to.atZone(java.time.ZoneId.systemDefault()).toInstant());
                } catch (Exception e) {
                    // Fallback to default dates if parsing fails
                    java.time.LocalDateTime startOfDay = java.time.LocalDate.now().minusDays(1).atStartOfDay();
                    fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    toDate = new java.util.Date();
                }
            }
            
            // Get all returns with detailed information
            List<GarmentReturn> returns = garmentReturnRepository.findAllWithDetails();
            
            // Get returns for the date range
            List<GarmentReturn> returnsInRange = garmentReturnRepository.findByReturnTimeBetween(fromDate, toDate);
            
            // Group returns by reason for better analysis
            Map<String, Long> returnsByReason = returnsInRange.stream()
                .collect(Collectors.groupingBy(
                    returnRecord -> returnRecord.getReturnReason() != null ? returnRecord.getReturnReason() : "Unknown",
                    Collectors.counting()
                ));
            
            // Group returns by department transition
            Map<String, Long> returnsByTransition = returnsInRange.stream()
                .collect(Collectors.groupingBy(
                    returnRecord -> returnRecord.getFromDepartment() + " → " + returnRecord.getToDepartment(),
                    Collectors.counting()
                ));
            
            // Get returns by department
            Map<Departments, Long> returnsByDepartment = new HashMap<>();
            for (Departments dept : Departments.values()) {
                long count = garmentReturnRepository.countReturnsByDepartmentBetween(dept, fromDate, toDate);
                returnsByDepartment.put(dept, count);
            }
            
            model.addAttribute("returns", returnsInRange);
            model.addAttribute("returnsByReason", returnsByReason);
            model.addAttribute("returnsByTransition", returnsByTransition);
            model.addAttribute("returnsByDepartment", returnsByDepartment);
            model.addAttribute("totalReturns", returnsInRange.size());
            model.addAttribute("from", fromDate.toString());
            model.addAttribute("to", toDate.toString());
            
            return "auth/admin/returns-dashboard";
            
        } catch (Exception e) {
            log.error("Error loading returns dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load returns dashboard: " + e.getMessage());
            return "auth/admin/returns-dashboard";
        }
    }

    @PostMapping("/add/users")
    public String addUser(User user) {
        return "auth/registration";
    }

    @PostMapping("/registration")
    public String registration(@ModelAttribute("RegistrationDto") RegistrationRequestDto registrationRequestDto,
                               BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "auth/registration";
        }
        userService.save(objectMapper.convertValue(registrationRequestDto, User.class));
        return "auth/login";
    }
    
    @PostMapping("/users/{userId}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable int userId, @RequestBody Map<String, Object> updates) {
        try {
            System.out.println("USER UPDATE DEBUG: Updating user ID: " + userId + " with updates: " + updates);
            
            // Debug: List all users first to see what IDs exist
            List<User> allUsers = userService.findAll();
            System.out.println("USER UPDATE DEBUG: All users in database:");
            for (User u : allUsers) {
                System.out.println("  - User ID: " + u.getId() + ", Name: " + u.getName() + ", Email: " + u.getEmail());
            }
            
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            System.out.println("USER UPDATE DEBUG: Found user: " + user.getName() + " (ID: " + user.getId() + ")");
            
            // Update user fields
            if (updates.containsKey("name")) {
                user.setName((String) updates.get("name"));
                System.out.println("USER UPDATE DEBUG: Updated name to: " + updates.get("name"));
            }
            if (updates.containsKey("email")) {
                user.setEmail((String) updates.get("email"));
                System.out.println("USER UPDATE DEBUG: Updated email to: " + updates.get("email"));
            }
            if (updates.containsKey("role")) {
                user.setRole(Role.valueOf((String) updates.get("role")));
                System.out.println("USER UPDATE DEBUG: Updated role to: " + updates.get("role"));
            }
            if (updates.containsKey("department")) {
                user.setDepartment(Departments.valueOf((String) updates.get("department")));
                System.out.println("USER UPDATE DEBUG: Updated department to: " + updates.get("department"));
            }
            if (updates.containsKey("scheduleTimes")) {
                @SuppressWarnings("unchecked")
                List<String> scheduleTimes = (List<String>) updates.get("scheduleTimes");
                user.setScheduleTimes(scheduleTimes);
                System.out.println("USER UPDATE DEBUG: Updated schedule times to: " + scheduleTimes);
            }
            
            userService.save(user);
            System.out.println("USER UPDATE DEBUG: User saved successfully");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User updated successfully");
            response.put("user", user);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("USER UPDATE DEBUG: Error updating user: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update user: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/users/{userId}/reset-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetUserPassword(@PathVariable int userId, @RequestBody Map<String, String> request) {
        try {
            System.out.println("PASSWORD RESET DEBUG: Resetting password for user ID: " + userId + " with request: " + request);

            // Debug: List all users first to see what IDs exist
            List<User> allUsers = userService.findAll();
            System.out.println("PASSWORD RESET DEBUG: All users in database:");
            for (User u : allUsers) {
                System.out.println("  - User ID: " + u.getId() + ", Name: " + u.getName() + ", Email: " + u.getEmail());
            }

            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            System.out.println("PASSWORD RESET DEBUG: Found user: " + user.getName() + " (ID: " + user.getId() + ")");
            
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new RuntimeException("New password is required");
            }
            
            System.out.println("PASSWORD RESET DEBUG: Setting new password for user: " + user.getName());
            
            // Reset password - encode it properly
            user.setPassword(newPassword);
            userService.save(user);
            
            System.out.println("PASSWORD RESET DEBUG: Password reset successfully for user: " + user.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("PASSWORD RESET DEBUG: Error resetting password: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to reset password: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/db-viewer")
    public ResponseEntity<Map<String, Object>> viewDatabase() {
        log.info("Database viewer accessed");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get orders
            List<Orders> orders = ordersRepository.findAll();
            Map<String, Object> ordersData = new HashMap<>();
            for (Orders order : orders) {
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("orderId", order.getOrderId());
                orderInfo.put("cleanCloudOrderId", order.getCleanCloudOrderId());
                orderInfo.put("customerName", order.getCustomerName());
                orderInfo.put("status", order.getStatus());
                orderInfo.put("createdAt", order.getCreatedAt());
                ordersData.put("Order_" + order.getOrderId(), orderInfo);
            }
            response.put("orders", ordersData);
            
            // Get garments
            List<Garments> garments = garmentRepository.findAll();
            Map<String, Object> garmentsData = new HashMap<>();
            for (Garments garment : garments) {
                Map<String, Object> garmentInfo = new HashMap<>();
                garmentInfo.put("garmentId", garment.getGarmentId());
                garmentInfo.put("cleanCloudGarmentId", garment.getCleanCloudGarmentId());
                garmentInfo.put("description", garment.getDescription());
                garmentInfo.put("departmentId", garment.getDepartmentId());
                garmentInfo.put("orderId", garment.getOrder() != null ? garment.getOrder().getOrderId() : null);
                garmentInfo.put("createdAt", garment.getCreatedAt());
                garmentsData.put("Garment_" + garment.getGarmentId(), garmentInfo);
            }
            response.put("garments", garmentsData);
            
            // Get users
            List<User> users = userService.findAll();
            Map<String, Object> usersData = new HashMap<>();
            for (User user : users) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("name", user.getName());
                userInfo.put("email", user.getEmail());
                userInfo.put("department", user.getDepartment());
                userInfo.put("role", user.getRole());
                usersData.put("User_" + user.getId(), userInfo);
            }
            response.put("users", usersData);
            
            // Get garment scans
            List<GarmentScan> scans = garmentScanRepository.findAll();
            Map<String, Object> scansData = new HashMap<>();
            for (GarmentScan scan : scans) {
                Map<String, Object> scanInfo = new HashMap<>();
                scanInfo.put("id", scan.getId());
                scanInfo.put("garmentId", scan.getGarment().getGarmentId());
                scanInfo.put("cleanCloudGarmentId", scan.getGarment().getCleanCloudGarmentId());
                scanInfo.put("userId", scan.getUser().getId());
                scanInfo.put("userName", scan.getUser().getName());
                scanInfo.put("department", scan.getDepartment());
                scanInfo.put("scannedAt", scan.getScannedAt());
                scansData.put("Scan_" + scan.getId(), scanInfo);
            }
            response.put("scans", scansData);
            
            response.put("success", true);
            response.put("message", "Database contents retrieved successfully");
            
        } catch (Exception e) {
            log.error("Error retrieving database contents: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/debug/users")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugUsers() {
        log.info("Debug users endpoint accessed");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<User> users = userService.findAll();
            response.put("totalUsers", users.size());
            response.put("users", users.stream().map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("name", user.getName());
                userInfo.put("email", user.getEmail());
                userInfo.put("role", user.getRole());
                userInfo.put("department", user.getDepartment());
                userInfo.put("state", user.getState());
                return userInfo;
            }).collect(Collectors.toList()));
            
            response.put("success", true);
            response.put("message", "Users debug info retrieved successfully");
            
        } catch (Exception e) {
            log.error("Error retrieving users debug info: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
