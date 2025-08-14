package com.chich.maqoor.repository;

import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.constant.Departments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface GarmentRepository extends JpaRepository<Garments, Integer> {

    List<Garments> findByOrder_OrderId(int orderId);

    List<Garments> findByDepartmentId(Departments department);

    @Query("SELECT g FROM Garments g WHERE g.lastUpdate BETWEEN :fromDate AND :toDate")
    List<Garments> findByLastUpdateBetween(@Param("fromDate") Date fromDate, @Param("toDate") Date toDate);

    @Query("SELECT COUNT(g) FROM Garments g WHERE g.lastUpdate BETWEEN :fromDate AND :toDate")
    long countByLastUpdateBetween(@Param("fromDate") Date fromDate, @Param("toDate") Date toDate);

    @Query("SELECT COUNT(g) FROM Garments g WHERE g.departmentId = :department")
    long countByDepartmentId(@Param("department") Departments department);

    Garments findByCleanCloudGarmentId(String cleanCloudGarmentId);

    Garments findByCleanCloudGarmentIdAndOrder_OrderId(String cleanCloudGarmentId, int orderId);

    void deleteByOrder_OrderId(int orderId);

    List<Garments> findAllByOrder_OrderId(int orderId);
}
