package com.chich.maqoor.repository;

import com.chich.maqoor.entity.Garments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarmentRepository extends JpaRepository<Garments,Integer> {
    java.util.List<Garments> findByOrder_OrderId(int orderId);
    java.util.List<Garments> findByDepartmentId(com.chich.maqoor.entity.constant.Departments department);
}
