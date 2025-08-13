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

    List<GarmentScan> findByGarment_GarmentId(int garmentId);

    @Query("select gs.user.id as userId, count(gs) as total from GarmentScan gs where gs.department = :department and gs.scannedAt between :from and :to group by gs.user.id")
    List<Object[]> countByUserInDepartmentBetween(@Param("department") Departments department,
                                                  @Param("from") Date from,
                                                  @Param("to") Date to);

    @Query("select gs.user.id as userId, gs.department as department, count(gs) as total from GarmentScan gs where gs.scannedAt between :from and :to group by gs.user.id, gs.department")
    List<Object[]> countByUserAndDepartmentBetween(@Param("from") Date from,
                                                   @Param("to") Date to);

    List<GarmentScan> findByUser_IdAndScannedAtBetweenOrderByScannedAtDesc(int userId, Date from, Date to);

    List<GarmentScan> findByScannedAtBetweenOrderByScannedAtAsc(Date from, Date to);
}


