package com.school.smart_attendance_backend.repository;

import com.school.smart_attendance_backend.entity.AttendanceLog;
import com.school.smart_attendance_backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    // Find logs for a student
    List<AttendanceLog> findByStudentId(Long studentId);

    // Check if attendance marked for student on given date (based on timestamp date)
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AttendanceLog a WHERE a.student = ?1 AND FUNCTION('DATE', a.timestamp) = ?2")
    boolean existsByStudentAndDate(Student student, LocalDate date);

    // Count distinct dates in attendance logs (count of unique attendance days)
    @Query("SELECT COUNT(DISTINCT FUNCTION('DATE', a.timestamp)) FROM AttendanceLog a")
    long countDistinctDates();

    // Count attendance records of a student
    @Query("SELECT COUNT(a) FROM AttendanceLog a WHERE a.student = ?1")
    long countByStudent(Student student);
}
