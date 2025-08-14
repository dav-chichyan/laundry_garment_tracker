package com.chich.maqoor.repository;

import com.chich.maqoor.entity.Task;
import com.chich.maqoor.entity.constant.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByTaskListIdOrderByPositionAsc(Long taskListId);
    
    List<Task> findByAssignedToId(Long userId);
    
    List<Task> findByCreatedById(Long userId);
    
    List<Task> findByStatus(TaskStatus status);
    
    List<Task> findByPriority(com.chich.maqoor.entity.constant.TaskPriority priority);
    
    @Query("SELECT t FROM Task t WHERE t.dueDate < :now AND t.status != 'DONE'")
    List<Task> findOverdueTasks(@Param("now") LocalDateTime now);
    
    @Query("SELECT t FROM Task t WHERE t.dueDate BETWEEN :now AND :tomorrow AND t.status != 'DONE'")
    List<Task> findDueSoonTasks(@Param("now") LocalDateTime now, @Param("tomorrow") LocalDateTime tomorrow);
    
    @Query("SELECT t FROM Task t WHERE t.title LIKE %:searchTerm% OR t.description LIKE %:searchTerm%")
    List<Task> searchTasks(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT MAX(t.position) FROM Task t WHERE t.taskList.id = :taskListId")
    Integer findMaxPositionByTaskListId(@Param("taskListId") Long taskListId);
    
    List<Task> findByLabelsContaining(String label);
}
