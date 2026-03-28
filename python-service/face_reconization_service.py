from flask import Flask, request, jsonify
from flask_cors import CORS
import cv2
import numpy as np
import face_recognition
import dlib
from scipy.spatial import distance
import os
import glob

app = Flask(__name__)
CORS(app)

detector = dlib.get_frontal_face_detector()
predictor = dlib.shape_predictor("shape_predictor_68_face_landmarks.dat")

EAR_THRESHOLD = 0.25
BLINK_CONSEC_FRAMES = 2

known_face_encodings = []
known_face_metadata = []

def calculate_ear(eye):
    A = distance.euclidean(eye[1], eye[5])
    B = distance.euclidean(eye[2], eye[4])
    C = distance.euclidean(eye[0], eye[3])
    ear = (A + B) / (2.0 * C)
    return ear

def detect_blinks(frame):
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = detector(gray)
    blink_count = 0
    for face in faces:
        landmarks = predictor(gray, face)
        left_eye = [(landmarks.part(n).x, landmarks.part(n).y) for n in range(36, 42)]
        right_eye = [(landmarks.part(n).x, landmarks.part(n).y) for n in range(42, 48)]
        left_ear = calculate_ear(left_eye)
        right_ear = calculate_ear(right_eye)
        ear = (left_ear + right_ear) / 2.0
        if ear < EAR_THRESHOLD:
            blink_count += 1
    return blink_count

def load_known_faces():
    global known_face_encodings, known_face_metadata
    known_face_encodings = []
    known_face_metadata = []

    uploads_path = "/home/aman-singh/IdeaProjects/smart-attendance-backend/src/main/resources/static/uploads/*.jpg"

    image_files = glob.glob(uploads_path)

    for image_path in image_files:
        try:
            filename = os.path.basename(image_path)
            roll_no = filename.split('_')[0]
            image = face_recognition.load_image_file(image_path)
            encodings = face_recognition.face_encodings(image)
            if encodings:
                known_face_encodings.append(encodings[0])
                known_face_metadata.append({'rollNo': roll_no, 'filename': filename})
        except Exception:
            pass

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'online',
        'known_faces_count': len(known_face_encodings)
    })

@app.route('/reload-faces', methods=['POST'])
def reload_faces():
    load_known_faces()
    return jsonify({
        'success': True,
        'message': f'Loaded {len(known_face_encodings)} known faces'
    })

@app.route('/detect', methods=['POST'])
def detect_face_and_blink():
    try:
        if 'image' not in request.files:
            return jsonify({'matched': False, 'error': 'No image provided'}), 400
        file = request.files['image']
        file_bytes = np.frombuffer(file.read(), np.uint8)
        frame = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)
        if frame is None:
            return jsonify({'matched': False, 'error': 'Invalid image format'}), 400
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        face_locations = face_recognition.face_locations(rgb_frame)
        face_encodings = face_recognition.face_encodings(rgb_frame, face_locations)
        if not face_encodings:
            return jsonify({'matched': False, 'message': 'No face detected in image'})

        matched_student = None
        for face_encoding in face_encodings:
            matches = face_recognition.compare_faces(known_face_encodings, face_encoding, tolerance=0.85)
            face_distances = face_recognition.face_distance(known_face_encodings, face_encoding)
            if len(face_distances) > 0:
                best_match_index = np.argmin(face_distances)
                if matches[best_match_index]:
                    matched_student = known_face_metadata[best_match_index]
                    break

        if not matched_student:
            return jsonify({'matched': False, 'message': 'Face not recognized in database'})

        blink_count = detect_blinks(frame)
        if blink_count < 1:
            return jsonify({'matched': False,
                            'message': 'Please blink your eyes for liveness verification',
                            'studentName': 'Unknown', 'blinkCount': blink_count})

        return jsonify({'matched': True,
                        'rollNo': matched_student['rollNo'],
                        'blinkCount': blink_count,
                        'message': 'Face recognized and liveness verified'})
    except Exception as e:
        return jsonify({'matched': False, 'error': str(e)}), 500

@app.route('/detect-sequence', methods=['POST'])
def detect_blink_sequence():
    try:
        files = request.files.getlist('images')
        frames = [cv2.imdecode(np.frombuffer(f.read(), np.uint8), cv2.IMREAD_COLOR) for f in files]
        if not frames:
            return jsonify({'matched': False, 'message': 'No frames received'}), 400

        rgb_frame = cv2.cvtColor(frames[0], cv2.COLOR_BGR2RGB)
        face_locations = face_recognition.face_locations(rgb_frame)
        face_encodings = face_recognition.face_encodings(rgb_frame, face_locations)

        matched_student = None
        if face_encodings:
            face_encoding = face_encodings[0]
            matches = face_recognition.compare_faces(known_face_encodings, face_encoding, tolerance=0.85)
            face_distances = face_recognition.face_distance(known_face_encodings, face_encoding)
            if len(face_distances) > 0:
                best_match_index = np.argmin(face_distances)
                if matches[best_match_index]:
                    matched_student = known_face_metadata[best_match_index]

        if not matched_student:
            return jsonify({'matched': False, 'message': 'Face not recognized in database'})

        consec_closed = 0
        blink_detected = False

        for frame in frames:
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = detector(gray)
            if not faces:
                continue
            face = faces[0]
            landmarks = predictor(gray, face)
            left_eye = [(landmarks.part(n).x, landmarks.part(n).y) for n in range(36, 42)]
            right_eye = [(landmarks.part(n).x, landmarks.part(n).y) for n in range(42, 48)]
            left_ear = calculate_ear(left_eye)
            right_ear = calculate_ear(right_eye)
            ear = (left_ear + right_ear) / 2.0
            if ear < EAR_THRESHOLD:
                consec_closed += 1
            else:
                if consec_closed >= BLINK_CONSEC_FRAMES:
                    blink_detected = True
                    break
                consec_closed = 0

        if not blink_detected:
            return jsonify({'matched': False,
                            'message': 'Please blink your eyes for liveness verification',
                            'studentName': matched_student.get('rollNo', 'Unknown')})

        return jsonify({'matched': True,
                        'rollNo': matched_student['rollNo'],
                        'message': 'Face recognized and liveness verified'})
    except Exception as e:
        return jsonify({'matched': False, 'error': str(e)}), 500

if __name__ == '__main__':
    predictor_path = "shape_predictor_68_face_landmarks.dat"
    if not os.path.exists(predictor_path):
        print("\n⚠️  WARNING: shape_predictor_68_face_landmarks.dat not found!")
        print("Download it from:")
        print("http://dlib.net/files/shape_predictor_68_face_landmarks.dat.bz2")
        print("Extract and place in the python-service folder\n")

    load_known_faces()
    app.run(host='0.0.0.0', port=5000, debug=False)
