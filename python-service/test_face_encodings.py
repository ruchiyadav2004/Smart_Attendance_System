import face_recognition

image_path = "/home/aman-singh/IdeaProjects/smart-attendance-backend/src/main/resources/static/uploads/2412000140016_1762634978592.jpg"
image = face_recognition.load_image_file(image_path)
encodings = face_recognition.face_encodings(image)
print(f"Number of face encodings found: {len(encodings)}")
