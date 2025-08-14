package com.chich.maqoor.repository;

import com.chich.maqoor.entity.GarmentReturn;
import com.chich.maqoor.entity.constant.Departments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface GarmentReturnRepository extends JpaRepository<GarmentReturn, Integer> {
    
    /**
     * Find returns by user
     */
    List<GarmentReturn> findByUser_Id(int userId);
    
    /**
     * Find returns by department
     */
    List<GarmentReturn> findByFromDepartment(Departments department);
    
    /**
     * Find returns by date range
     */
    List<GarmentReturn> findByReturnTimeBetween(Date fromDate, Date toDate);
    
    /**
     * Find returns by user and date range
     */
    List<GarmentReturn> findByUser_IdAndReturnTimeBetween(int userId, Date fromDate, Date toDate);
    
    /**
     * Count returns by user in date range
     */
    @Query("SELECT COUNT(r) FROM GarmentReturn r WHERE r.user.id = :userId AND r.returnTime BETWEEN :fromDate AND :toDate")
    long countReturnsByUserBetween(@Param("userId") int userId, @Param("fromDate") Date fromDate, @Param("toDate") Date toDate);
    
    /**
     * Count returns by department in date range
     */
    @Query("SELECT COUNT(r) FROM GarmentReturn r WHERE r.fromDepartment = :department AND r.returnTime BETWEEN :fromDate AND :toDate")
    long countReturnsByDepartmentBetween(@Param("department") Departments department, @Param("fromDate") Date fromDate, @Param("toDate") Date toDate);
    
    /**
     * Find returns by garment
     */
    List<GarmentReturn> findByGarment_GarmentId(int garmentId);
}
