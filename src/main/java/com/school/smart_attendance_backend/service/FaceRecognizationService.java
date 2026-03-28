package com.school.smart_attendance_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to communicate with Python Flask service
 * for face recognition and blink detection
 */
@Service
public class FaceRecognizationService {

    @Value("${python.service.url:http://localhost:5000}")
    private String pythonServiceUrl;

    private final RestTemplate restTemplate;

    public FaceRecognizationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Send image to Python service for face detection and blink detection
     *
     * @param image - The captured image from camera
     * @return Map with detection results
     */
    public Map<String, Object> detectFaceAndBlink(MultipartFile image) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            // Call Python Flask API
            String url = pythonServiceUrl + "/detect";
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                result.put("matched", false);
                result.put("error", "Python service returned error");
                return result;
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("matched", false);
            result.put("error", "Failed to connect to Python service: " + e.getMessage());
            return result;
        }
    }
}
