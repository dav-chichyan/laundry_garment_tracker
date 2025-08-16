package com.chich.maqoor.controller.admin;

import com.chich.maqoor.dto.TaskCommentRequestDto;
import com.chich.maqoor.dto.TaskCreateRequestDto;
import com.chich.maqoor.dto.TaskListCreateRequestDto;
import com.chich.maqoor.entity.*;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.repository.TaskAttachmentRepository;
import com.chich.maqoor.repository.TaskCommentRepository;
import com.chich.maqoor.repository.TaskListRepository;
import com.chich.maqoor.repository.TaskRepository;
import com.chich.maqoor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin")
public class TaskManagementController {

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCommentRepository taskCommentRepository;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private UserService userService;

    private static final String UPLOAD_DIR = "uploads/tasks/";

    @GetMapping("/task-management")
    public String taskManagementPage(Model model) {
        List<TaskList> taskLists = taskListRepository.findAllByOrderByPositionAsc();
        List<User> allUsers = userService.findAll();
        List<User> adminUsers = allUsers.stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .toList();
        
        // Debug logging
        System.out.println("TASK MANAGEMENT DEBUG: Total users found: " + allUsers.size());
        System.out.println("TASK MANAGEMENT DEBUG: Admin users found: " + adminUsers.size());
        adminUsers.forEach(user -> System.out.println("TASK MANAGEMENT DEBUG: Admin user - ID: " + user.getId() + ", Name: " + user.getName() + ", Role: " + user.getRole()));
        
        // Add sample data if no lists exist
        if (taskLists.isEmpty()) {
            taskLists = createSampleData();
        }
        
        model.addAttribute("taskLists", taskLists);
        model.addAttribute("adminUsers", adminUsers);
        model.addAttribute("priorities", com.chich.maqoor.entity.constant.TaskPriority.values());
        model.addAttribute("statuses", com.chich.maqoor.entity.constant.TaskStatus.values());
        
        return "auth/admin/task-management";
    }
    
    @GetMapping("/tasks")
    public String tasksListPage() {
        // Redirect to the existing task management page
        return "redirect:/admin/task-management";
    }
    
    private List<TaskList> createSampleData() {
        List<TaskList> sampleLists = new ArrayList<>();
        
        // Create sample lists
        TaskList todoList = new TaskList();
        todoList.setId(1L);
        todoList.setName("To Do");
        todoList.setDescription("Tasks to be started");
        todoList.setPosition(0);
        todoList.setCreatedAt(LocalDateTime.now());
        todoList.setUpdatedAt(LocalDateTime.now());
        
        TaskList inProgressList = new TaskList();
        inProgressList.setId(2L);
        inProgressList.setName("In Progress");
        inProgressList.setDescription("Tasks currently being worked on");
        inProgressList.setPosition(1);
        inProgressList.setCreatedAt(LocalDateTime.now());
        inProgressList.setUpdatedAt(LocalDateTime.now());
        
        TaskList reviewList = new TaskList();
        reviewList.setId(3L);
        reviewList.setName("Review");
        reviewList.setDescription("Tasks ready for review");
        reviewList.setPosition(2);
        reviewList.setCreatedAt(LocalDateTime.now());
        reviewList.setUpdatedAt(LocalDateTime.now());
        
        TaskList doneList = new TaskList();
        doneList.setId(4L);
        doneList.setName("Done");
        doneList.setDescription("Completed tasks");
        doneList.setPosition(3);
        doneList.setCreatedAt(LocalDateTime.now());
        doneList.setUpdatedAt(LocalDateTime.now());
        
        sampleLists.add(todoList);
        sampleLists.add(inProgressList);
        sampleLists.add(reviewList);
        sampleLists.add(doneList);
        
        return sampleLists;
    }

    @PostMapping("/task-lists/create")
    @ResponseBody
    public TaskList createTaskList(@RequestBody TaskListCreateRequestDto requestDto) {
        TaskList taskList = new TaskList();
        taskList.setName(requestDto.getName());
        taskList.setDescription(requestDto.getDescription());
        
        // Set position at the end
        Integer maxPosition = taskListRepository.findMaxPosition();
        taskList.setPosition(maxPosition != null ? maxPosition + 1 : 0);
        
        return taskListRepository.save(taskList);
    }

    @PostMapping("/tasks/create")
    @ResponseBody
    public Task createTask(@RequestBody TaskCreateRequestDto requestDto) {
        Task task = new Task();
        task.setTitle(requestDto.getTitle());
        task.setDescription(requestDto.getDescription());
        task.setDueDate(requestDto.getDueDate());
        task.setPriority(requestDto.getPriority());
        task.setStatus(requestDto.getStatus());
        task.setLabels(requestDto.getLabels());
        
        // Set task list
        TaskList taskList = taskListRepository.findById(requestDto.getTaskListId())
                .orElseThrow(() -> new RuntimeException("Task list not found"));
        task.setTaskList(taskList);
        
        // Set assigned user if provided
        if (requestDto.getAssignedToId() != null) {
            User assignedUser = userService.findById(requestDto.getAssignedToId().intValue())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            task.setAssignedTo(assignedUser);
        }
        
        // Set created by (current user)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        task.setCreatedBy(currentUser);
        
        // Set position at the end of the list
        Integer maxPosition = taskRepository.findMaxPositionByTaskListId(requestDto.getTaskListId());
        task.setPosition(maxPosition != null ? maxPosition + 1 : 0);
        
        return taskRepository.save(task);
    }

    @PostMapping("/tasks/{taskId}/comment")
    @ResponseBody
    public TaskComment addComment(@PathVariable Long taskId, @RequestBody TaskCommentRequestDto requestDto) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        TaskComment comment = new TaskComment();
        comment.setContent(requestDto.getContent());
        comment.setTask(task);
        
        // Set created by (current user)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        comment.setCreatedBy(currentUser);
        
        return taskCommentRepository.save(comment);
    }

    @PostMapping("/tasks/{taskId}/upload")
    @ResponseBody
    public TaskAttachment uploadAttachment(@PathVariable Long taskId, @RequestParam("file") MultipartFile file) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + fileExtension;
            
            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            
            // Create attachment record
            TaskAttachment attachment = new TaskAttachment();
            attachment.setFilename(filename);
            attachment.setOriginalFilename(originalFilename);
            attachment.setFilePath(filePath.toString());
            attachment.setFileSize(file.getSize());
            attachment.setMimeType(file.getContentType());
            attachment.setTask(task);
            
            // Set uploaded by (current user)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            attachment.setUploadedBy(currentUser);
            
            return taskAttachmentRepository.save(attachment);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @PutMapping("/tasks/{taskId}/status")
    @ResponseBody
    public Task updateTaskStatus(@PathVariable Long taskId, @RequestParam String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        task.setStatus(com.chich.maqoor.entity.constant.TaskStatus.valueOf(status));
        return taskRepository.save(task);
    }

    @PutMapping("/tasks/{taskId}/assign")
    @ResponseBody
    public Task assignTask(@PathVariable Long taskId, @RequestParam Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        User user = userService.findById(userId.intValue())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        task.setAssignedTo(user);
        return taskRepository.save(task);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseBody
    public String deleteTask(@PathVariable Long taskId) {
        taskRepository.deleteById(taskId);
        return "Task deleted successfully";
    }

    @DeleteMapping("/task-lists/{listId}")
    @ResponseBody
    public String deleteTaskList(@PathVariable Long listId) {
        taskListRepository.deleteById(listId);
        return "Task list deleted successfully";
    }

    @GetMapping("/tasks/search")
    public String searchTasks(@RequestParam String query, Model model) {
        List<Task> tasks = taskRepository.searchTasks(query);
        model.addAttribute("tasks", tasks);
        model.addAttribute("searchQuery", query);
        return "auth/admin/task-search-results";
    }
}
