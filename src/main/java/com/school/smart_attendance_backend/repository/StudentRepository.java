package com.school.smart_attendance_backend.repository;
import com.school.smart_attendance_backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    // Custom query: Find by roll number
    Optional<Student> findByRollNo(String rollNo);
}