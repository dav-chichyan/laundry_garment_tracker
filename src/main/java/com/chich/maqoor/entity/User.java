package com.chich.maqoor.entity;

import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.entity.constant.Role;
import com.chich.maqoor.entity.constant.UserState;
import com.chich.maqoor.entity.constant.UserStatus;
import jakarta.persistence.*;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Data
@Getter
@Setter
public class User  {

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;


    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private UserState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = true)
    private Departments department;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_schedule_times", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "schedule_time")
    private List<String> scheduleTimes;

    @Column(name = "username")
    private String username;

    // Additional getters and setters for new fields
    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public List<String> getScheduleTimes() {
        return scheduleTimes;
    }

    public void setScheduleTimes(List<String> scheduleTimes) {
        this.scheduleTimes = scheduleTimes;
    }
    
    // Helper method to get department names for display
    public String getDepartmentNames() {
        if (department != null) {
            return department.name();
        }
        return "No Department";
    }
    
    // Helper method to get departments as a comma-separated string
    public String getDepartmentsAsString() {
        if (department != null) {
            return department.name();
        }
        return "";
    }
    
    // Helper method to get all departments as a formatted string for display
    public String getAllDepartmentsDisplay() {
        if (department != null) {
            return department.name();
        }
        return "No Department";
    }
    
    // Helper method to get all departments as a comma-separated string for data attributes
    public String getAllDepartmentsData() {
        if (department != null) {
            return department.name();
        }
        return "";
    }
}
