package com.school.smart_attendance_backend.controller;

import com.school.smart_attendance_backend.entity.AttendanceLog;
import com.school.smart_attendance_backend.entity.Student;
import com.school.smart_attendance_backend.repository.AttendanceLogRepository;
import com.school.smart_attendance_backend.repository.StudentRepository;
import com.school.smart_attendance_backend.service.FaceRecognizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to connect
public class AttendanceController {

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private AttendanceLogRepository logRepo;

    @Autowired
    private FaceRecognizationService faceRecognitionService;

    // Root welcome page
    @GetMapping("/")
    public String home() {
        return "<h1>Smart Attendance Backend Active!</h1>" +
                "<p><a href='/api/students'>View Students</a></p>";
    }

    // ===== STUDENT MANAGEMENT =====

    @PostMapping(value = "/students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addStudent(
            @RequestParam String name,
            @RequestParam String rollNo,
            @RequestParam(required = false) String email,
            @RequestParam(value = "photo", required = true) MultipartFile photo) {

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Student> existing = studentRepo.findByRollNo(rollNo);
            if (existing.isPresent()) {
                response.put("success", false);
                response.put("error", "Student with roll number " + rollNo + " already exists");
                return ResponseEntity.badRequest().body(response);
            }

            Student student = new Student();
            student.setName(name);
            student.setRollNo(rollNo);
            if (email != null && !email.isEmpty()) {
                student.setEmail(email);
            }

            if (photo != null && !photo.isEmpty()) {
                Path uploadDir = Paths.get("src/main/resources/static/uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                String fileName = rollNo + "_" + System.currentTimeMillis() + ".jpg";
                Path filePath = uploadDir.resolve(fileName);
                photo.transferTo(filePath);
                student.setPhotoPath("/uploads/" + fileName);
            }

            student = studentRepo.save(student);

            response.put("success", true);
            response.put("message", "Student added successfully");
            response.put("studentId", student.getId());
            response.put("name", student.getName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/students")
    public ResponseEntity<List<Student>> getStudents() {
        return ResponseEntity.ok(studentRepo.findAll());
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<Student> getStudent(@PathVariable Long id) {
        return studentRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // New endpoint to mark attendance by roll number and return attendance data
    @PostMapping("/attendance/mark")
    public ResponseEntity<?> markAttendanceByRollNo(@RequestParam String rollNo) {
        Map<String, Object> response = new HashMap<>();

        Optional<Student> studentOpt = studentRepo.findByRollNo(rollNo);
        if (!studentOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Student not found");
            return ResponseEntity.badRequest().body(response);
        }

        Student student = studentOpt.get();
        LocalDate today = LocalDate.now();

        boolean alreadyMarked = logRepo.existsByStudentAndDate(student, today);
        if (!alreadyMarked) {
            AttendanceLog log = new AttendanceLog();
            log.setStudent(student);
            log.setStatus("Present"); // Assuming you track status
            logRepo.save(log);
        }

        long totalDays = logRepo.countDistinctDates();
        long presentDays = logRepo.countByStudent(student);

        double attendancePercentage = totalDays == 0 ? 0 : ((double) presentDays / totalDays) * 100;

        response.put("success", true);
        response.put("name", student.getName());
        response.put("rollNo", student.getRollNo());
        response.put("attendancePercent", Math.round(attendancePercentage * 10) / 10.0);
        response.put("lastAttendance", today.toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Main endpoint: Detect face and mark attendance with image
     */
    @PostMapping(value = "/detect-attendance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectAttendance(
            @RequestParam("image") MultipartFile image) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (image == null || image.isEmpty()) {
                response.put("success", false);
                response.put("error", "No image provided");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> pythonResult = faceRecognitionService.detectFaceAndBlink(image);

            boolean matched = (Boolean) pythonResult.get("matched");

            if (!matched) {
                response.put("success", false);
                response.put("message", "Face not recognized or no blinks detected");
                response.put("details", pythonResult);
                return ResponseEntity.ok(response);
            }

            String rollNo = (String) pythonResult.get("rollNo");
            String studentName = (String) pythonResult.get("studentName");
            Integer blinkCount = (Integer) pythonResult.get("blinkCount");

            Optional<Student> studentOpt = studentRepo.findByRollNo(rollNo);
            if (!studentOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Student not found in database");
                return ResponseEntity.badRequest().body(response);
            }

            Student student = studentOpt.get();

            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            List<AttendanceLog> todayLogs = logRepo.findByStudentId(student.getId());
            boolean alreadyMarked = todayLogs.stream()
                    .anyMatch(log -> log.getTimestamp().isAfter(todayStart));

            if (alreadyMarked) {
                response.put("success", false);
                response.put("message", "Attendance already marked today for " + studentName);
                response.put("studentName", studentName);
                return ResponseEntity.ok(response);
            }

            AttendanceLog log = new AttendanceLog();
            log.setStudent(student);
            log.setStatus("Present");
            log.setBlinkCount(blinkCount);
            log.setTimestamp(LocalDateTime.now());
            logRepo.save(log);

            response.put("success", true);
            response.put("message", "Attendance marked successfully!");
            response.put("studentId", student.getId());
            response.put("studentName", studentName);
            response.put("rollNo", rollNo);
            response.put("blinkCount", blinkCount);
            response.put("timestamp", log.getTimestamp());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Error processing attendance: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ===== ATTENDANCE LOGS =====

    @GetMapping("/logs")
    public ResponseEntity<List<AttendanceLog>> getAllLogs() {
        return ResponseEntity.ok(logRepo.findAll());
    }

    @GetMapping("/logs/student/{studentId}")
    public ResponseEntity<List<AttendanceLog>> getStudentLogs(@PathVariable Long studentId) {
        return ResponseEntity.ok(logRepo.findByStudentId(studentId));
    }
}
