package com.chich.maqoor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TaskCommentRequestDto {
    
    @NotBlank(message = "Comment content is required")
    @Size(min = 1, max = 1000, message = "Comment must be between 1 and 1000 characters")
    private String content;
    
    @NotNull(message = "Task ID is required")
    private Long taskId;
    
    // Constructors
    public TaskCommentRequestDto() {}
    
    public TaskCommentRequestDto(String content, Long taskId) {
        this.content = content;
        this.taskId = taskId;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}
