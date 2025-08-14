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

@Component
public class DataSeeder implements CommandLineRunner {

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
            return;
        }

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

        User maria = new User();
        maria.setName("Maria Washing");
        maria.setEmail("maria@example.com");
        maria.setPassword(passwordEncoder.encode("maria123"));
        maria.setRole(Role.USER);
        maria.setState(UserState.ACTIVE);
        maria.setDepartment(Departments.WASHING);
        maria.setUsername("maria@example.com");
        maria = userRepository.save(maria);

        // Order with garments
        Orders order = new Orders();
        // Don't set orderId manually - let it be generated
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

        Garments g1 = new Garments();
        // Don't set garmentId manually - let it be generated
        g1.setCleanCloudGarmentId("GAR-101");
        g1.setOrder(savedOrder);
        g1.setDescription("White Shirt");
        g1.setType("Shirt");
        g1.setColor("White");
        g1.setSize("M");
        g1.setSpecialInstructions("Handle with care");
        g1.setDepartmentId(Departments.RECEPTION);
        g1.setLastUpdate(new Date());
        g1.setCreatedAt(new Date());

        Garments g2 = new Garments();
        // Don't set garmentId manually - let it be generated
        g2.setCleanCloudGarmentId("GAR-102");
        g2.setOrder(savedOrder);
        g2.setDescription("Blue Dress");
        g2.setType("Dress");
        g2.setColor("Blue");
        g2.setSize("S");
        g2.setSpecialInstructions("Gentle wash only");
        g2.setDepartmentId(Departments.RECEPTION);
        g2.setLastUpdate(new Date());
        g2.setCreatedAt(new Date());

        Garments g3 = new Garments();
        // Don't set garmentId manually - let it be generated
        g3.setCleanCloudGarmentId("GAR-103");
        g3.setOrder(savedOrder);
        g3.setDescription("Black Coat");
        g3.setType("Coat");
        g3.setColor("Black");
        g3.setSize("L");
        g3.setSpecialInstructions("Dry clean only");
        g3.setDepartmentId(Departments.RECEPTION);
        g3.setLastUpdate(new Date());
        g3.setCreatedAt(new Date());

        garmentRepository.saveAll(Arrays.asList(g1, g2, g3));

        // Scans timeline
        saveScan(g1, ana, Departments.RECEPTION);
        saveScan(g1, maria, Departments.WASHING);
        saveScan(g1, ana, Departments.IRONING);
        saveScan(g1, ana, Departments.DELIVERY);
        // Return: after delivery goes back to ironing
        saveScan(g1, ana, Departments.IRONING);

        saveScan(g2, maria, Departments.WASHING);
        saveScan(g2, ana, Departments.IRONING);
        saveScan(g2, ana, Departments.DELIVERY);

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


