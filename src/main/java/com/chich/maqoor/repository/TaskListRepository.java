package com.chich.maqoor.repository;

import com.chich.maqoor.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    
    List<TaskList> findAllByOrderByPositionAsc();
    
    @Query("SELECT MAX(tl.position) FROM TaskList tl")
    Integer findMaxPosition();
    
    List<TaskList> findByNameContainingIgnoreCase(String name);
}
