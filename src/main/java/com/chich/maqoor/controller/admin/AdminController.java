package com.chich.maqoor.controller.admin;


import com.chich.maqoor.dto.RegistrationRequestDto;
import com.chich.maqoor.entity.GarmentScan;
import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.User;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.repository.GarmentScanRepository;
import com.chich.maqoor.repository.GarmentRepository;
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

    @GetMapping("/users")
    public String usersDashboard(Model model,
                                 @RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") java.time.LocalDateTime from,
                                 @RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") java.time.LocalDateTime to,
                                 @RequestParam(value = "dept", required = false) Departments sortDepartment,
                                 @RequestParam(value = "sort", required = false, defaultValue = "desc") String sortDirection,
                                 @RequestParam(value = "only", required = false, defaultValue = "false") boolean onlyDepartment) {
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

        List<User> users = new java.util.ArrayList<>(userService.findAll());
        List<Object[]> rows = garmentScanRepository.countByUserAndDepartmentBetween(fromDate, toDate);
        Map<Integer, Map<String, Long>> userDeptCounts = new HashMap<>();
        for (Object[] r : rows) {
            Integer userId = (Integer) r[0];
            Departments dept = (Departments) r[1];
            Long total = (Long) r[2];
            userDeptCounts.computeIfAbsent(userId, k -> new HashMap<>()).put(dept.name(), total);
        }

        // Simple returns heuristic: count garments that visited DELIVERY and then returned to any prior department
        Map<Integer, Long> returnsByUser = new HashMap<>();
        List<GarmentScan> all = garmentScanRepository.findByScannedAtBetweenOrderByScannedAtAsc(fromDate, toDate);
        Map<Integer, Departments> lastDeptByGarment = new HashMap<>();
        for (GarmentScan s : all) {
            Departments last = lastDeptByGarment.get(s.getGarment().getGarmentId());
            if (last == Departments.DELIVERY && s.getDepartment() != Departments.DELIVERY) {
                returnsByUser.merge(s.getUser().getId(), 1L, Long::sum);
            }
            lastDeptByGarment.put(s.getGarment().getGarmentId(), s.getDepartment());
        }

        // Optional filtering and sorting by department
        if (sortDepartment != null) {
            if (onlyDepartment) {
                users.removeIf(u -> {
                    Map<String, Long> counts = userDeptCounts.get(u.getId());
                    Long c = counts != null ? counts.getOrDefault(sortDepartment.name(), 0L) : 0L;
                    return c == 0L;
                });
            }
            java.util.Comparator<User> comparator = java.util.Comparator.comparingLong(u -> {
                Map<String, Long> counts = userDeptCounts.get(u.getId());
                return counts != null ? counts.getOrDefault(sortDepartment.name(), 0L) : 0L;
            });
            if (!"asc".equalsIgnoreCase(sortDirection)) {
                comparator = comparator.reversed();
            }
            users.sort(comparator);
        }

        model.addAttribute("users", users);
        model.addAttribute("userDeptCounts", userDeptCounts);
        model.addAttribute("returnsByUser", returnsByUser);
        model.addAttribute("departments", Departments.values());
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
        model.addAttribute("activeDept", sortDepartment);
        model.addAttribute("sortDir", sortDirection);
        model.addAttribute("onlyDept", onlyDepartment);
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
        User user = userService.findById(id);
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
