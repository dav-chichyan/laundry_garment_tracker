package com.chich.maqoor.repository;

import com.chich.maqoor.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    
    List<TaskAttachment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    
    List<TaskAttachment> findByUploadedById(Long userId);
    
    List<TaskAttachment> findByMimeTypeStartingWith(String mimeTypePrefix);
}
