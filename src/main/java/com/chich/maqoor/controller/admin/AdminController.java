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
import com.chich.maqoor.repository.ProblemRepository;
import com.chich.maqoor.entity.Problem;
import com.chich.maqoor.entity.constant.ProblemStatus;
import com.chich.maqoor.service.UserService;
import com.chich.maqoor.service.GarmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import org.springframework.http.HttpStatus;
import java.util.stream.Collectors;

// add import
import com.chich.maqoor.service.CleanCloudService;
import com.chich.maqoor.dto.CleanCloudOrderDetails;

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

    // new: CleanCloud service
    @Autowired
    private CleanCloudService cleanCloudService;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public String usersDashboard(Model model,
                                 @RequestParam(value = "fromDate", required = false) String fromDateStr,
                                 @RequestParam(value = "toDate", required = false) String toDateStr,
                                 @RequestParam(value = "filter", required = false) String filter) {
        Date fromDate;
        Date toDate;
        
        // Handle quick filter buttons
        if (filter != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            java.time.LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
            
            switch (filter) {
                case "today":
                    fromDate = java.util.Date.from(startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    toDate = java.util.Date.from(endOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    break;
                case "yesterday":
                    java.time.LocalDateTime yesterdayStart = startOfDay.minusDays(1);
                    java.time.LocalDateTime yesterdayEnd = endOfDay.minusDays(1);
                    fromDate = java.util.Date.from(yesterdayStart.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    toDate = java.util.Date.from(yesterdayEnd.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    break;
                default:
                    // Default to yesterday to today
                    fromDate = java.util.Date.from(startOfDay.minusDays(1).atZone(java.time.ZoneId.systemDefault()).toInstant());
                    toDate = new java.util.Date();
                    break;
            }
        } else if (fromDateStr == null || toDateStr == null) {
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
        
        // Get real-time department counts (exclude hidden departments)
        Map<Departments, Long> departmentCounts = new HashMap<>();
        for (Departments dept : getVisibleDepartments()) {
            long count = garmentRepository.countByDepartmentId(dept);
            departmentCounts.put(dept, count);
        }
        
        // Get user performance data (scans per department)
        Map<Integer, Map<Departments, Long>> userDeptCounts = new HashMap<>();
        for (User user : users) {
            Map<Departments, Long> userCounts = new HashMap<>();
            for (Departments dept : getVisibleDepartments()) {
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
        // Compute returns per user within the selected range
        Map<Integer, Long> returnsByUser = new HashMap<>();
        for (User user : users) {
            long rcount = garmentReturnRepository.countReturnsByUserBetween(user.getId(), fromDate, toDate);
            returnsByUser.put(user.getId(), rcount);
        }
        
        // Format dates for form inputs
        java.time.LocalDateTime fromLocal = fromDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        java.time.LocalDateTime toLocal = toDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        
        model.addAttribute("users", users);
        model.addAttribute("departments", getVisibleDepartments());
        model.addAttribute("from", fromLocal.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        model.addAttribute("to", toLocal.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        model.addAttribute("currentFilter", filter);
        model.addAttribute("departmentCounts", departmentCounts);
        model.addAttribute("userDeptCounts", userDeptCounts);
        model.addAttribute("scansByUser", scansByUser);
        model.addAttribute("returnsByUser", returnsByUser);
        
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

    @GetMapping("/department-garments")
    public String departmentGarmentsPage(Model model) {
        model.addAttribute("departments", getVisibleDepartments());
        return "auth/admin/department-garments";
    }

    @GetMapping("/departments")
    public String departmentsOverview(Model model) {
        // Build counts per department for overview cards - only for visible departments
        Map<Departments, Long> departmentCounts = new HashMap<>();
        Map<String, Long> departmentCountsByName = new HashMap<>();
        
        // Only count visible departments (same as what frontend displays)
        Departments[] visible = getVisibleDepartments();
        
        for (Departments dept : visible) {
            long count = 0L;
            try {
                // Use the same method as individual department pages for consistency
                List<Garments> garments = garmentRepository.findByDepartmentId(dept);
                count = garments.size();
                log.info("Department {} - Garment count: {}", dept, count);
            } catch (Exception e) {
                log.error("Error getting count for department {}: {}", dept, e.getMessage());
            }
            departmentCounts.put(dept, count);
            departmentCountsByName.put(dept.name(), count);
        }
        
        long totalGarments = departmentCounts.values().stream().mapToLong(Long::longValue).sum();
        
        // Debug logging
        log.info("DEPARTMENTS OVERVIEW DEBUG: Visible departments: {}", java.util.Arrays.toString(visible));
        log.info("DEPARTMENTS OVERVIEW DEBUG: Department counts: {}", departmentCounts);
        
        model.addAttribute("departments", visible);
        model.addAttribute("departmentCounts", departmentCounts);
        model.addAttribute("departmentCountsByName", departmentCountsByName);
        model.addAttribute("totalGarments", totalGarments);
        model.addAttribute("numDepartments", visible.length);
        return "auth/admin/departments-overview";
    }

    // Problems list page
    @GetMapping("/problems")
    public String problemsPage(Model model,
                               @RequestParam(value = "status", required = false) ProblemStatus status,
                               @RequestParam(value = "department", required = false) Departments dept,
                               @RequestParam(value = "staff", required = false) Integer staffId,
                               @RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "dateFrom", required = false) String dateFrom,
                               @RequestParam(value = "dateTo", required = false) String dateTo,
                               @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
                               @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder) {
        
        List<Problem> problems = problemRepository.findAll();
        
        // Apply filters
        if (status != null) {
            problems = problems.stream().filter(p -> p.getStatus() == status).toList();
        }
        if (dept != null) {
            problems = problems.stream().filter(p -> p.getDepartment() == dept).toList();
        }
        if (staffId != null) {
            problems = problems.stream().filter(p -> p.getUser() != null && p.getUser().getId() == staffId).toList();
        }
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            problems = problems.stream().filter(p -> 
                (p.getNotes() != null && p.getNotes().toLowerCase().contains(searchLower)) ||
                (p.getOrder() != null && String.valueOf(p.getOrder().getOrderId()).contains(search))
            ).toList();
        }
        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            try {
                java.time.LocalDate fromDate = java.time.LocalDate.parse(dateFrom);
                problems = problems.stream().filter(p -> 
                    p.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().isAfter(fromDate.minusDays(1))
                ).toList();
            } catch (Exception e) {
                log.warn("Invalid dateFrom format: {}", dateFrom);
            }
        }
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            try {
                java.time.LocalDate toDate = java.time.LocalDate.parse(dateTo);
                problems = problems.stream().filter(p -> 
                    p.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(toDate.plusDays(1))
                ).toList();
            } catch (Exception e) {
                log.warn("Invalid dateTo format: {}", dateTo);
            }
        }
        
        // Apply sorting
        java.util.Comparator<Problem> comparator = switch (sortBy) {
            case "department" -> java.util.Comparator.comparing(p -> p.getDepartment().name());
            case "user.name" -> java.util.Comparator.comparing(p -> p.getUser() != null ? p.getUser().getName() : "");
            default -> java.util.Comparator.comparing(Problem::getCreatedAt);
        };
        
        if ("asc".equals(sortOrder)) {
            problems = problems.stream().sorted(comparator).toList();
        } else {
            problems = problems.stream().sorted(comparator.reversed()).toList();
        }
        
        // Add attributes to model
        model.addAttribute("problems", problems);
        model.addAttribute("departments", getVisibleDepartments());
        model.addAttribute("statuses", ProblemStatus.values());
        model.addAttribute("search", search);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("department", dept);
        model.addAttribute("staff", staffId);
        
        try {
            // Load all non-admin users for staff dropdown
            List<User> allUsers = userService.findAll();
            List<User> staffOnly = allUsers == null ? java.util.Collections.emptyList() :
                    allUsers.stream()
                            .filter(u -> u != null && u.getRole() != null && u.getRole() == Role.USER)
                            .toList();
            model.addAttribute("users", staffOnly);
        } catch (Exception ignored) {
            model.addAttribute("users", java.util.Collections.emptyList());
        }
        
        return "auth/admin/problems";
    }

    // Create a new problem
    @PostMapping("/problems")
    public String createProblem(@RequestParam("orderId") Integer orderId,
                                @RequestParam("department") Departments department,
                                @RequestParam("userId") Integer userId,
                                @RequestParam(value = "status", required = false) ProblemStatus status,
                                @RequestParam(value = "notes", required = false) String notes,
                                @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
                                @RequestParam(value = "type", required = false) com.chich.maqoor.entity.constant.AttachmentType type,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            log.info("CREATE PROBLEM: Received request - orderId={}, department={}, userId={}, status={}", 
                     orderId, department, userId, status);
            
            // Create order reference (no validation - allow any order ID)
            Orders order = new Orders();
            order.setOrderId(orderId);
            
            // Validate user
            User user = userService.findById(userId).orElse(null);
            if (user == null) {
                log.error("CREATE PROBLEM: User not found with ID: {}", userId);
                redirectAttributes.addFlashAttribute("error", "User not found with ID: " + userId);
                return "redirect:/admin/problems";
            }
            
            // Create and save problem
            Problem p = new Problem();
            p.setOrder(order);
            p.setDepartment(department);
            p.setUser(user);
            p.setStatus(status != null ? status : ProblemStatus.OPEN);
            p.setNotes(notes);
            p.setCreatedAt(new Date());
            p.setUpdatedAt(new Date());
            
            p = problemRepository.save(p);
            log.info("CREATE PROBLEM: Successfully created problem with ID: {}", p.getId());

            // Handle file upload
            try {
                if (file != null && !file.isEmpty()) {
                    log.info("CREATE PROBLEM: Processing file upload: {}", file.getOriginalFilename());
                    String uploadsDir = "src/main/resources/static/uploads";
                    java.nio.file.Files.createDirectories(java.nio.file.Path.of(uploadsDir));
                    String ext = org.springframework.util.StringUtils.getFilenameExtension(file.getOriginalFilename());
                    String filename = "problem-" + p.getId() + "-" + System.currentTimeMillis() + (ext != null ? ("." + ext) : "");
                    java.nio.file.Path dest = java.nio.file.Path.of(uploadsDir, filename);
                    file.transferTo(dest.toFile());
                    
                    // Append file link to notes
                    String link = "/uploads/" + filename;
                    String updatedNotes = (p.getNotes() != null ? p.getNotes() + "\n" : "") + 
                                         "Attachment (" + (type != null ? type.name() : "FILE") + "): " + link;
                    p.setNotes(updatedNotes);
                    problemRepository.save(p);
                    log.info("CREATE PROBLEM: File uploaded successfully: {}", filename);
                }
            } catch (Exception e) {
                log.error("CREATE PROBLEM: Error uploading file: {}", e.getMessage(), e);
            }
            
            redirectAttributes.addFlashAttribute("success", "Problem created successfully!");
            return "redirect:/admin/problems";
            
        } catch (Exception e) {
            log.error("CREATE PROBLEM: Error creating problem: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error creating problem: " + e.getMessage());
            return "redirect:/admin/problems";
        }
    }

    // Helper: Exclude departments not needed for UI (e.g., PACKAGING, LOCKER)
    private Departments[] getVisibleDepartments() {
        return java.util.Arrays.stream(Departments.values())
                .filter(d -> d != Departments.PACKAGING && d != Departments.LOCKER)
                .toArray(Departments[]::new);
    }

    @GetMapping("/debug/department-counts")
    @ResponseBody
    public Map<String, Object> debugDepartmentCounts() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> counts = new HashMap<>();
        
        for (Departments dept : Departments.values()) {
            long count = 0L;
            try {
                Integer c = garmentRepository.countByDepartmentIdInteger(dept);
                log.info("DEBUG: Department {} - Repository count: {}", dept, c);
                if (c != null) count = c.longValue();
            } catch (Exception e) {
                log.error("DEBUG: Error getting count for department {}: {}", dept, e.getMessage());
            }
            counts.put(dept.name(), count);
        }
        
        result.put("counts", counts);
        result.put("totalGarments", garmentRepository.count());
        
        return result;
    }

    @GetMapping("/orders/search")
    public String orderSearchPage(@RequestParam(value = "orderId", required = false) String orderIdParam, Model model) {
        if (orderIdParam != null) {
            String trimmed = orderIdParam.trim();
            if (!trimmed.isEmpty()) {
                try {
                    int orderId = Integer.parseInt(trimmed);
                    return searchOrder(orderId, model);
                } catch (NumberFormatException ex) {
                    model.addAttribute("error", "Please enter a valid numeric Order ID.");
                }
            }
        }
        return "auth/admin/order-search";
    }

    @PutMapping("/orders/{orderId}/state")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateOrderState(@PathVariable int orderId, @RequestBody Map<String, String> request) {
        log.info("Received request to update order state for order ID: {}, request: {}", orderId, request);
        Map<String, Object> response = new HashMap<>();
        try {
            String orderStateStr = request.get("orderState");
            log.info("Order state from request: {}", orderStateStr);
            
            if (orderStateStr == null || orderStateStr.isEmpty()) {
                log.warn("Order state is missing from request");
                response.put("success", false);
                response.put("message", "Order state is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Find order by internal ID
            Optional<Orders> orderOpt = ordersRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                log.warn("Order not found with ID: {}", orderId);
                response.put("success", false);
                response.put("message", "Order not found with ID: " + orderId);
                return ResponseEntity.badRequest().body(response);
            }

            Orders order = orderOpt.get();
            log.info("Found order: ID={}, CleanCloud ID={}, Current State={}", 
                     order.getOrderId(), order.getCleanCloudOrderId(), order.getOrderState());
            
            // Update order state
            com.chich.maqoor.entity.constant.OrderState newState = com.chich.maqoor.entity.constant.OrderState.valueOf(orderStateStr);
            order.setOrderState(newState);
            order.setUpdatedAt(new Date());
            ordersRepository.save(order);

            log.info("Order {} (CleanCloud ID: {}) state updated to {} by admin", 
                     orderId, order.getCleanCloudOrderId(), newState);

            response.put("success", true);
            response.put("message", "Order state updated successfully");
            response.put("newState", newState.name());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid order state value: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Invalid order state: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error updating order state for order {}: {}", orderId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to update order state: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/garments/{garmentId}/department")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateGarmentDepartment(@PathVariable int garmentId, @RequestBody Map<String, String> request) {
        log.info("Received request to update garment department for garment ID: {}, request: {}", garmentId, request);
        Map<String, Object> response = new HashMap<>();
        try {
            String departmentStr = request.get("department");
            log.info("Department from request: {}", departmentStr);
            
            if (departmentStr == null || departmentStr.isEmpty()) {
                log.warn("Department is missing from request");
                response.put("success", false);
                response.put("message", "Department is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Find garment by internal ID
            Optional<Garments> garmentOpt = garmentRepository.findById(garmentId);
            if (!garmentOpt.isPresent()) {
                log.warn("Garment not found with ID: {}", garmentId);
                response.put("success", false);
                response.put("message", "Garment not found with ID: " + garmentId);
                return ResponseEntity.badRequest().body(response);
            }

            Garments garment = garmentOpt.get();
            log.info("Found garment: ID={}, CleanCloud ID={}, Current Department={}", 
                     garment.getGarmentId(), garment.getCleanCloudGarmentId(), garment.getDepartmentId());
            
            // Update garment department
            Departments newDepartment = Departments.valueOf(departmentStr);
            garment.setDepartmentId(newDepartment);
            garment.setLastUpdate(new Date());
            garmentRepository.save(garment);

            log.info("Garment {} (CleanCloud ID: {}) department updated from {} to {} by admin", 
                     garmentId, garment.getCleanCloudGarmentId(), garment.getDepartmentId(), newDepartment);

            response.put("success", true);
            response.put("message", "Garment department updated successfully");
            response.put("newDepartment", newDepartment.name());
            response.put("garmentId", garmentId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid department value: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Invalid department: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error updating garment department for garment {}: {}", garmentId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to update garment department: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String searchOrder(int orderId, Model model) {
        System.out.println("Searching for order: " + orderId);
        try {
            // First, find the order by cleanCloudOrderId
            Optional<Orders> order = ordersRepository.findByCleanCloudOrderId(orderId);
            if (!order.isPresent()) {
                System.out.println("Order with cleanCloudOrderId " + orderId + " not found");
                model.addAttribute("error", "Order not found with ID: " + orderId);
                // Keep the entered value so the user sees what they searched
                model.addAttribute("orderId", orderId);
                return "auth/admin/order-search";
            }
            
            // Then get garments for that order from DB
            List<Garments> garments = garmentService.listByOrderId(order.get().getOrderId());

            // Enrich garment type from CleanCloud summary (getOrder)
            try {
                System.out.println("DEBUG: Starting CleanCloud enrichment for order " + orderId);
                CleanCloudOrderDetails cc = cleanCloudService.getOrder(orderId);
                System.out.println("DEBUG: CleanCloud order details for " + orderId + ": " + (cc != null ? "found" : "null"));
                if (cc != null) {
                    System.out.println("DEBUG: Summary from CleanCloud: '" + cc.getSummary() + "'");
                }
                if (cc != null && cc.getSummary() != null && garments != null && !garments.isEmpty()) {
                    List<String> summaryTypes = parseTypesFromSummary(cc.getSummary());
                    System.out.println("DEBUG: Parsed summary types: " + summaryTypes);
                    if (!summaryTypes.isEmpty()) {
                        // sort DB garments by ascending CleanCloud garment ID (numeric if possible)
                        List<Garments> sorted = new ArrayList<>(garments);
                        sorted.sort((a,b) -> {
                            try {
                                int ia = Integer.parseInt(a.getCleanCloudGarmentId());
                                int ib = Integer.parseInt(b.getCleanCloudGarmentId());
                                return Integer.compare(ia, ib);
                            } catch (Exception e) {
                                return String.valueOf(a.getCleanCloudGarmentId()).compareTo(String.valueOf(b.getCleanCloudGarmentId()));
                            }
                        });
                        System.out.println("DEBUG: Sorted garments by CleanCloud ID: " + 
                            sorted.stream().map(g -> g.getCleanCloudGarmentId() + ":" + g.getType()).collect(Collectors.toList()));
                        int n = Math.min(summaryTypes.size(), sorted.size());
                        for (int i=0;i<n;i++) {
                            String t = summaryTypes.get(i);
                            if (t != null && !t.isBlank()) {
                                Garments g = sorted.get(i);
                                System.out.println("DEBUG: Mapping type '" + t + "' to garment ID " + g.getCleanCloudGarmentId() + " (current type: '" + g.getType() + "')");
                                if (g.getType() == null || g.getType().isBlank() || "Unknown".equalsIgnoreCase(g.getType())) {
                                    g.setType(t.trim());
                                    try { 
                                        garmentRepository.save(g); 
                                        System.out.println("DEBUG: Successfully saved garment " + g.getCleanCloudGarmentId() + " with type '" + g.getType() + "'");
                                    } catch(Exception ex) {
                                        System.err.println("DEBUG: Failed to save garment " + g.getCleanCloudGarmentId() + ": " + ex.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to enrich garment types from CleanCloud for order " + orderId + ": " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("Found " + garments.size() + " garments for order " + orderId);
            if (garments != null) {
                for (Garments g : garments) {
                    System.out.println("Garment: " + g.getGarmentId() + ", Dept: " + g.getDepartmentId());
                }
            }
            model.addAttribute("orderId", orderId);
            model.addAttribute("order", order.get());
            model.addAttribute("garments", garments);
            System.out.println("Returning template: auth/admin/order-search (inline results)");
            return "auth/admin/order-search";
        } catch (Exception e) {
            System.err.println("Error searching order: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Order not found: " + e.getMessage());
            model.addAttribute("orderId", orderId);
            return "auth/admin/order-search";
        }
    }

    private List<String> parseTypesFromSummary(String summary) {
        List<String> result = new ArrayList<>();
        if (summary == null || summary.isBlank()) return result;
        String normalized = summary.replace("<br />", "<br>").replace("<br/>", "<br>").replace("\r\n", "\n").replace("\r", "\n");
        String[] items = normalized.split("<br>");
        java.util.regex.Pattern qtyPattern = java.util.regex.Pattern.compile("(?i)\\bx\\s*\\d+");
        for (String part : items) {
            if (part == null) continue;
            String line = part.trim();
            if (line.isEmpty()) continue;
            java.util.regex.Matcher m = qtyPattern.matcher(line);
            String name = m.find() ? line.substring(0, m.start()).trim() : line.trim();
            if (!name.isEmpty()) result.add(name);
        }
        return result;
    }

    @GetMapping("/departments/{department}")
    public String departmentGarments(@PathVariable Departments department, Model model) {
        List<Garments> garments = garmentRepository.findByDepartmentId(department);
        // Build a map of garmentId -> last scanning staff name for this department
        Map<Integer, String> staffByGarmentId = new HashMap<>();
        for (Garments g : garments) {
            try {
                List<GarmentScan> scans = garmentScanRepository.findByGarment_GarmentId(g.getGarmentId());
                Optional<GarmentScan> latestInDept = scans.stream()
                        .filter(s -> s != null && s.getDepartment() == department)
                        .filter(s -> s.getScannedAt() != null)
                        .max(Comparator.comparing(GarmentScan::getScannedAt));
                latestInDept.ifPresent(s -> {
                    if (s.getUser() != null) {
                        staffByGarmentId.put(g.getGarmentId(), s.getUser().getName());
                    }
                });
                // If nothing found, leave null (will render as N/A)
            } catch (Exception ignored) { }
        }
        model.addAttribute("department", department);
        model.addAttribute("garments", garments);
        model.addAttribute("staffByGarmentId", staffByGarmentId);
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
        try {
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
            try {
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
            } catch (Exception e) {
                log.error("Error fetching scans from database", e);
                scans = new ArrayList<>();
            }
            
            // Filter out any scans with null users, garments, or timestamps to prevent template errors
            if (scans != null) {
                scans = scans.stream()
                    .filter(scan -> scan != null
                        && scan.getUser() != null
                        && scan.getGarment() != null
                        && scan.getScannedAt() != null)
                    .collect(Collectors.toList());
            } else {
                scans = new ArrayList<>();
            }
            
            // Get summary statistics safely
            Map<Departments, Long> scansByDepartment = new HashMap<>();
            Map<Integer, Long> scansByUser = new HashMap<>();
            
            for (Departments dept : Departments.values()) {
                long count = scans.stream()
                    .filter(scan -> scan.getDepartment() == dept)
                    .count();
                scansByDepartment.put(dept, count);
            }
            
            List<User> allUsers = new ArrayList<>();
            try {
                allUsers = userService.findAll();
            } catch (Exception e) {
                log.error("Error fetching users", e);
            }
            
            for (User user : allUsers) {
                long count = scans.stream()
                    .filter(scan -> scan.getUser() != null && scan.getUser().getId() == user.getId())
                    .count();
                scansByUser.put(user.getId(), count);
            }
            
            // Distinct counts for template (avoid complex Thymeleaf expressions)
            long distinctDepartmentsCount = scans.stream()
                .map(GarmentScan::getDepartment)
                .filter(Objects::nonNull)
                .distinct()
                .count();
            long uniqueGarmentsCount = scans.stream()
                .map(scan -> scan.getGarment() != null ? scan.getGarment().getCleanCloudGarmentId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();
            
            model.addAttribute("scans", scans);
            model.addAttribute("from", fromDate);
            model.addAttribute("to", toDate);
            model.addAttribute("users", allUsers);
            model.addAttribute("scansByDepartment", scansByDepartment);
            model.addAttribute("scansByUser", scansByUser);
            model.addAttribute("distinctDepartmentsCount", distinctDepartmentsCount);
            model.addAttribute("uniqueGarmentsCount", uniqueGarmentsCount);
            
            // Add error scans data (placeholder for now - you can implement actual error logging)
            List<Map<String, Object>> errorScans = new ArrayList<>();
            
            // TODO: Implement actual error scan logging from your system
            // For demonstration, adding some sample error data
            Map<String, Object> sampleError1 = new HashMap<>();
            sampleError1.put("id", "E001");
            sampleError1.put("garmentId", "20238");
            sampleError1.put("userName", "John Doe");
            sampleError1.put("department", "RECEPTION");
            sampleError1.put("errorTime", new Date());
            sampleError1.put("errorType", "Scan Failed");
            sampleError1.put("errorMessage", "Barcode not recognized");
            sampleError1.put("status", "Resolved");
            errorScans.add(sampleError1);
            
            Map<String, Object> sampleError2 = new HashMap<>();
            sampleError2.put("id", "E002");
            sampleError2.put("garmentId", "20240");
            sampleError2.put("userName", "Jane Smith");
            sampleError2.put("department", "IRONING");
            sampleError2.put("errorTime", new Date(System.currentTimeMillis() - 3600000)); // 1 hour ago
            sampleError2.put("errorType", "Duplicate Scan");
            sampleError2.put("errorMessage", "Garment already scanned in this department");
            sampleError2.put("status", "Pending");
            errorScans.add(sampleError2);
            
            model.addAttribute("errorScans", errorScans);
            
            log.info("Scanned garments page loaded successfully with {} scans", scans.size());
            
            return "auth/admin/scanned-garments";
        } catch (Exception e) {
            log.error("Error loading scanned garments page", e);
            model.addAttribute("error", "Error loading scanned garments data: " + e.getMessage());
            model.addAttribute("scans", new ArrayList<>());
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("scansByDepartment", new HashMap<>());
            model.addAttribute("scansByUser", new HashMap<>());
            model.addAttribute("errorScans", new ArrayList<>());
            return "auth/admin/scanned-garments";
        }
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
                    returnRecord -> formatDepartmentName(returnRecord.getFromDepartment()) + " → " + formatDepartmentName(returnRecord.getToDepartment()),
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
            user.setPassword(passwordEncoder.encode(newPassword));
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

    @GetMapping("/debug/add-reception-120")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addReceptionTester() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1) Ensure a test user exists
            User tester = userService.findAll().stream()
                    .filter(u -> "Reception Tester".equals(u.getName()))
                    .findFirst()
                    .orElse(null);
            if (tester == null) {
                tester = new User();
                tester.setName("Reception Tester");
                tester.setEmail("reception.tester@example.com");
                tester.setRole(Role.USER);
                tester.setDepartment(Departments.RECEPTION);
                tester.setStatus(com.chich.maqoor.entity.constant.UserStatus.ACTIVE);
                userService.save(tester);
            }

            // 2) Ensure there is at least one garment to attach scans to
            Garments garment = garmentRepository.findAll().stream().findFirst().orElse(null);
            if (garment == null) {
                Orders order = new Orders();
                order.setOrderNumber("TEST-RECEP-120");
                order.setCustomerName("Test Customer");
                order.setStatus("PENDING");
                order.setCreatedAt(new Date());
                order.setUpdatedAt(new Date());
                order = ordersRepository.save(order);

                garment = new Garments();
                garment.setOrder(order);
                garment.setCleanCloudGarmentId("TEST-G-1");
                garment.setDescription("Test Garment");
                garment.setDepartmentId(Departments.RECEPTION);
                garment.setCreatedAt(new Date());
                garment.setLastUpdate(new Date());
                garment = garmentRepository.save(garment);
            }

            // 3) Create 120 scans in RECEPTION within the current day
            Date now = new Date();
            for (int i = 0; i < 120; i++) {
                GarmentScan scan = new GarmentScan();
                scan.setGarment(garment);
                scan.setUser(tester);
                scan.setDepartment(Departments.RECEPTION);
                // Stagger timestamps backward by 2 minutes each
                scan.setScannedAt(new Date(now.getTime() - (long) i * 120_000L));
                garmentScanRepository.save(scan);
            }

            response.put("success", true);
            response.put("message", "Created/updated Reception Tester with 120 RECEPTION scans");
            response.put("userId", tester.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Format department names for UI display
     * - STAIN_REMOVAL becomes STAIN
     * - DRY_CLEANING becomes DRY
     */
    private String formatDepartmentName(Departments dept) {
        if (dept == null) {
            return "N/A";
        }
        switch (dept) {
            case STAIN_REMOVAL:
                return "STAIN";
            case DRY_CLEANING:
                return "DRY";
            default:
                return dept.name();
        }
    }
}
