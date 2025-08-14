package com.chich.maqoor.repository;

import com.chich.maqoor.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    
    List<TaskComment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    
    List<TaskComment> findByCreatedById(Long userId);
}
