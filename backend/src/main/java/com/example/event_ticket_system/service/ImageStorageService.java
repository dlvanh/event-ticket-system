package com.example.event_ticket_system.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final RestClient externalRestClient;

    @Value("${imgbb.api.key:}")
    private String imgbbApiKey;

    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ảnh không được để trống.");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ cho phép upload file ảnh (jpg, png, gif, webp)");
        }
    }

    public String uploadImage(MultipartFile file) {
        validateImageFile(file);
        if (imgbbApiKey == null || imgbbApiKey.isBlank()) {
            throw new IllegalStateException("IMGBB_API_KEY is not configured");
        }
        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("image", base64Image);

            JsonNode response = externalRestClient.post()
                    .uri("https://api.imgbb.com/1/upload?key={key}", imgbbApiKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("data").path("url").isMissingNode()) {
                throw new IllegalStateException("ImgBB returned an invalid response");
            }
            String imageUrl = response.path("data").path("url").asText();
            log.debug("Uploaded image to ImgBB: {}", imageUrl);
            return imageUrl;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image file for upload", e);
        }
    }
}
