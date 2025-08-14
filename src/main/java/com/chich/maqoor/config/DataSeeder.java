package com.chich.maqoor.config;

import com.chich.maqoor.entity.*;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.entity.constant.UserState;
import com.chich.maqoor.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private GarmentRepository garmentRepository;

    @Autowired
    private GarmentScanRepository garmentScanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0L || ordersRepository.count() > 0L || garmentRepository.count() > 0L) {
            log.info("Data already exists, skipping data seeding");
            return;
        }

        log.info("Starting data seeding...");

        try {
            // Admin User
            User admin = new User();
            admin.setName("Admin User");
            admin.setEmail("admin@maqoor.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setState(UserState.ACTIVE);
            admin.setDepartment(Departments.RECEPTION);
            admin.setUsername("admin@maqoor.com");
            admin = userRepository.save(admin);
            log.info("Created admin user: {}", admin.getEmail());

            // Users
            User ana = new User();
            ana.setName("Ana Ironing");
            ana.setEmail("ana@example.com");
            ana.setPassword(passwordEncoder.encode("ana123"));
            ana.setRole(Role.USER);
            ana.setState(UserState.ACTIVE);
            ana.setDepartment(Departments.IRONING);
            ana.setUsername("ana@example.com");
            ana = userRepository.save(ana);
            log.info("Created user: {}", ana.getEmail());

            User maria = new User();
            maria.setName("Maria Washing");
            maria.setEmail("maria@example.com");
            maria.setPassword(passwordEncoder.encode("maria123"));
            maria.setRole(Role.USER);
            maria.setState(UserState.ACTIVE);
            maria.setDepartment(Departments.WASHING);
            maria.setUsername("maria@example.com");
            maria = userRepository.save(maria);
            log.info("Created user: {}", maria.getEmail());

            // Order with garments
            Orders order = new Orders();
            order.setCleanCloudOrderId(44886); // Test CleanCloud order ID
            order.setOrderNumber("ORD-1001");
            order.setCustomerName("John Doe");
            order.setCustomerPhone("+1234567890");
            order.setPickupDate("2025-01-15");
            order.setDeliveryDate("2025-01-17");
            order.setStatus("PENDING");
            order.setType("DRY_CLEAN");
            order.setExpress(false);
            order.setCreatedAt(new Date());
            order.setUpdatedAt(new Date());
            Orders savedOrder = ordersRepository.save(order);
            log.info("Created test order: {}", savedOrder.getOrderId());

            // Create garments
            List<Garments> garments = Arrays.asList(
                createGarment("GAR-101", "White Shirt", "Shirt", "White", "M", "Handle with care", savedOrder),
                createGarment("GAR-102", "Blue Dress", "Dress", "Blue", "S", "Gentle wash only", savedOrder),
                createGarment("GAR-103", "Black Coat", "Coat", "Black", "L", "Dry clean only", savedOrder)
            );
            
            garmentRepository.saveAll(garments);
            log.info("Created {} test garments", garments.size());

            // Create scan timeline
            createScanTimeline(garments, ana, maria);
            
            log.info("Data seeding completed successfully");
            
        } catch (Exception e) {
            log.error("Error during data seeding: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
        }
    }
    
    private Garments createGarment(String cleanCloudId, String description, String type, String color, String size, String instructions, Orders order) {
        Garments garment = new Garments();
        garment.setCleanCloudGarmentId(cleanCloudId);
        garment.setOrder(order);
        garment.setDescription(description);
        garment.setType(type);
        garment.setColor(color);
        garment.setSize(size);
        garment.setSpecialInstructions(instructions);
        garment.setDepartmentId(Departments.RECEPTION);
        garment.setLastUpdate(new Date());
        garment.setCreatedAt(new Date());
        return garment;
    }
    
    private void createScanTimeline(List<Garments> garments, User ana, User maria) {
        // Create scan timeline for first garment
        Garments g1 = garments.get(0);
        saveScan(g1, ana, Departments.RECEPTION);
        saveScan(g1, maria, Departments.WASHING);
        saveScan(g1, ana, Departments.IRONING);
        saveScan(g1, ana, Departments.DELIVERY);
        saveScan(g1, ana, Departments.IRONING); // Return after delivery
        
        // Create scan timeline for second garment
        Garments g2 = garments.get(1);
        saveScan(g2, maria, Departments.WASHING);
        saveScan(g2, ana, Departments.IRONING);
        saveScan(g2, ana, Departments.DELIVERY);
        
        // Create scan timeline for third garment
        Garments g3 = garments.get(2);
        saveScan(g3, maria, Departments.WASHING);
    }

    private void saveScan(Garments garment, User user, Departments department) {
        garment.setDepartmentId(department);
        garment.setLastUpdate(new Date());
        garmentRepository.save(garment);

        GarmentScan scan = new GarmentScan();
        scan.setGarment(garment);
        scan.setUser(user);
        scan.setDepartment(department);
        scan.setScannedAt(new Date());
        garmentScanRepository.save(scan);
    }
}


