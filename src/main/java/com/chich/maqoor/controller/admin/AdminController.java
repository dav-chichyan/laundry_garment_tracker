package com.chich.maqoor.controller.admin;


import com.chich.maqoor.dto.RegistrationRequestDto;
import com.chich.maqoor.entity.GarmentScan;
import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.GarmentReturn;
import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
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

@Controller
@RequestMapping("/admin")
public class AdminController {

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
        
        // Get returns data for the date range
        List<GarmentReturn> returns = garmentReturnRepository.findByReturnTimeBetween(fromDate, toDate);
        
        // Get returns by department
        Map<Departments, Long> returnsByDepartment = new HashMap<>();
        for (Departments dept : Departments.values()) {
            long count = garmentReturnRepository.countReturnsByDepartmentBetween(dept, fromDate, toDate);
            returnsByDepartment.put(dept, count);
        }
        
        // Get returns by user - initialize with 0 for all users
        Map<Integer, Long> returnsByUser = new HashMap<>();
        for (User user : users) {
            long count = garmentReturnRepository.countReturnsByUserBetween(user.getId(), fromDate, toDate);
            returnsByUser.put(user.getId(), count);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("departments", Departments.values());
        model.addAttribute("from", fromDate.toString());
        model.addAttribute("to", toDate.toString());
        model.addAttribute("departmentCounts", departmentCounts);
        model.addAttribute("userDeptCounts", userDeptCounts);
        model.addAttribute("returns", returns);
        model.addAttribute("returnsByDepartment", returnsByDepartment);
        model.addAttribute("returnsByUser", returnsByUser);
        return "auth/admin/users-management";
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
        model.addAttribute("user", user);
        model.addAttribute("scans", scans);
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
        return "auth/admin/user-detail";
    }

    @GetMapping("/orders/search")
    public String searchOrder(@RequestParam("orderId") int orderId, Model model) {
        System.out.println("Searching for order: " + orderId);
        try {
            List<Garments> garments = garmentService.listByOrderId(orderId);
            System.out.println("Found " + garments.size() + " garments for order " + orderId);
            if (garments != null) {
                for (Garments g : garments) {
                    System.out.println("Garment: " + g.getGarmentId() + ", Dept: " + g.getDepartmentId());
                }
            }
            model.addAttribute("orderId", orderId);
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
        model.addAttribute("from", fromDate.toString());
        model.addAttribute("to", toDate.toString());
        model.addAttribute("departments", Departments.values());
        
        return "auth/admin/returns-detail";
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
}
