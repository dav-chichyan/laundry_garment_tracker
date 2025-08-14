package com.chich.maqoor.dto;

import com.chich.maqoor.entity.constant.TaskPriority;
import com.chich.maqoor.entity.constant.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class TaskCreateRequestDto {
    
    @NotBlank(message = "Task title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    private LocalDateTime dueDate;
    
    private TaskPriority priority;
    
    private TaskStatus status;
    
    private Long assignedToId;
    
    @NotNull(message = "Task list is required")
    private Long taskListId;
    
    private List<String> labels;
    
    // Constructors
    public TaskCreateRequestDto() {}
    
    public TaskCreateRequestDto(String title, String description, LocalDateTime dueDate, 
                               TaskPriority priority, TaskStatus status, Long assignedToId, 
                               Long taskListId, List<String> labels) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.assignedToId = assignedToId;
        this.taskListId = taskListId;
        this.labels = labels;
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Long getAssignedToId() {
        return assignedToId;
    }
    
    public void setAssignedToId(Long assignedToId) {
        this.assignedToId = assignedToId;
    }
    
    public Long getTaskListId() {
        return taskListId;
    }
    
    public void setTaskListId(Long taskListId) {
        this.taskListId = taskListId;
    }
    
    public List<String> getLabels() {
        return labels;
    }
    
    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
}
