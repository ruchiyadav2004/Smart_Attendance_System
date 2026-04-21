# Smart Attendance System

Camera-based smart attendance system built with **Spring Boot + MySQL** and a separate **Python Flask + OpenCV** service for face recognition and eye‑blink (liveness) detection.

---

## Features

- Student registration with photo stored in MySQL.
- Browser-based camera capture for attendance.
- Face recognition + eye‑blink detection before marking attendance.
- Attendance logs and per‑student attendance statistics.
- Simple HTML pages for attendance and registration.

---

## Tech Stack

- **Backend:** Java 17, Spring Boot, Spring Data JPA, Maven  
- **Database:** MySQL  
- **AI Service:** Python, Flask, OpenCV, face_recognition, dlib  
- **Frontend:** HTML, CSS, JavaScript (served from Spring Boot)

---

## Prerequisites

User must have:

- Git  
- Java 17+ & Maven  
- Python 3.9+  
- MySQL Server  
- Web browser with camera & a webcam

(Installation steps are standard and not specific to this project.)

---

## How to run (quick steps)

1. **Clone the repo**
  git clone https://github.com/ruchiyadav2004/smart-attendance-backend.git
  cd smart-attendance-backend


2. **Configure MySQL**

- Create a database (e.g. `smart_attendance`).  
- Update `src/main/resources/application.properties` with your DB URL, username, and password.

3. **Run Spring Boot backend**
    mvn spring-boot:run

   
4. **Run Python face-recognition service**

    cd python-service
    python -m venv venv

    activate venv, then:
    pip install -r requirements.txt
    python face_reconization_service.py

   
5. **Open in browser**

- Attendance page: `http://localhost:8080/attendance.html`  
- Registration page: `http://localhost:8080/register.html`

---

## Project structure

- `src/main/java/com.school.smart_attendance_backend/` – Spring Boot code (controllers, entities, repositories, services)  
- `src/main/resources/static/` – `attendance.html`, `register.html` and static assets  
- `python-service/` – Flask + face recognition code and model files

---

## Future work

- Docker setup for one‑click run  
- Cloud deployment (Railway / Render)  
- Role‑based authentication (admin / teacher / student)
