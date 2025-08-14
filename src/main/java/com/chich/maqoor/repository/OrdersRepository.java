package com.chich.maqoor.repository;

import com.chich.maqoor.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {
    
    /**
     * Find order by CleanCloud order ID
     */
    Optional<Orders> findByCleanCloudOrderId(int cleanCloudOrderId);
    
    /**
     * Check if order exists by CleanCloud order ID
     */
    boolean existsByCleanCloudOrderId(int cleanCloudOrderId);
    
    /**
     * Find order by order number
     */
    Optional<Orders> findByOrderNumber(String orderNumber);
}
