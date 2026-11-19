package com.example.event_ticket_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
    private Object errors;
    private Instant timestamp;

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return build(data, message, null, HttpStatus.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return build(data, message, null, HttpStatus.CREATED);
    }

    public static ResponseEntity<ApiResponse<Void>> error(String message, HttpStatus status) {
        return build(null, message, null, status);
    }

    public static ResponseEntity<ApiResponse<Void>> error(String message, Object errors, HttpStatus status) {
        return build(null, message, errors, status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(T data, String message, Object errors, HttpStatus status) {
        ApiResponse<T> body = ApiResponse.<T>builder()
                .status(status.value())
                .message(message)
                .data(data)
                .errors(errors)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
