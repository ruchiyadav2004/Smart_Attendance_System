package com.school.smart_attendance_backend.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String rollNo;

    private String email;

    private String photoPath; // Path to student's reference photo

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<AttendanceLog> attendanceLogs;

    // Default constructor
    public Student() {}

    // Getters and Setters for all fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public List<AttendanceLog> getAttendanceLogs() {
        return attendanceLogs;
    }

    public void setAttendanceLogs(List<AttendanceLog> attendanceLogs) {
        this.attendanceLogs = attendanceLogs;
    }
}
