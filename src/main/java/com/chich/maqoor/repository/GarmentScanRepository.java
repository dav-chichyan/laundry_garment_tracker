package com.chich.maqoor.repository;

import com.chich.maqoor.entity.GarmentScan;
import com.chich.maqoor.entity.constant.Departments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface GarmentScanRepository extends JpaRepository<GarmentScan, Integer> {

    List<GarmentScan> findByUser_Id(int userId);

    List<GarmentScan> findByUser_IdAndScannedAtBetweenOrderByScannedAtDesc(int userId, Date fromDate, Date toDate);

    List<GarmentScan> findByGarment_GarmentId(int garmentId);

    @Query("SELECT gs.user.id, COUNT(gs) FROM GarmentScan gs WHERE gs.department = :department AND gs.scannedAt BETWEEN :fromDate AND :toDate GROUP BY gs.user.id")
    List<Object[]> countByUserInDepartmentBetween(@Param("department") Departments department, @Param("fromDate") Date fromDate, @Param("toDate") Date toDate);

    @Query("SELECT COUNT(gs) FROM GarmentScan gs WHERE gs.user.id = :userId AND gs.department = :department AND gs.scannedAt BETWEEN :fromDate AND :toDate")
    long countByUser_IdAndDepartmentAndScannedAtBetween(@Param("userId") int userId, @Param("department") Departments department, @Param("fromDate") Date fromDate, @Param("toDate") Date toDate);

    @Query("SELECT COUNT(gs) FROM GarmentScan gs WHERE gs.scannedAt BETWEEN :fromDate AND :toDate")
    long countByScannedAtBetween(@Param("fromDate") Date fromDate, @Param("toDate") Date toDate);
}


