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
        order.setOrderId(1001);
        order.setType("DRY_CLEAN");
        order.setExpress(false);
        ordersRepository.save(order);

        Garments g1 = new Garments();
        g1.setGarmentId(101);
        g1.setOrder(order);
        g1.setDescription("White Shirt");
        g1.setDepartmentId(Departments.RECEPTION);
        g1.setLastUpdate(new Date());

        Garments g2 = new Garments();
        g2.setGarmentId(102);
        g2.setOrder(order);
        g2.setDescription("Blue Dress");
        g2.setDepartmentId(Departments.RECEPTION);
        g2.setLastUpdate(new Date());

        Garments g3 = new Garments();
        g3.setGarmentId(103);
        g3.setOrder(order);
        g3.setDescription("Black Coat");
        g3.setDepartmentId(Departments.RECEPTION);
        g3.setLastUpdate(new Date());

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


