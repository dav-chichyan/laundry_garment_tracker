package com.chich.maqoor.repository;

import com.chich.maqoor.entity.Problem;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.ProblemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Integer> {
    List<Problem> findByStatus(ProblemStatus status);
    List<Problem> findByDepartment(Departments department);
}


