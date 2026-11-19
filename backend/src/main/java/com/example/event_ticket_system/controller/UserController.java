package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.request.DeleteRequest;
import com.example.event_ticket_system.dto.request.EventRejectionReasonRequest;
import com.example.event_ticket_system.dto.request.UpdateProfileRequest;
import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.dto.response.UserResponseDto;
import com.example.event_ticket_system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Void>> deleteUsers(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest.getIds() == null || deleteRequest.getIds().isEmpty()) {
            throw new IllegalArgumentException("The data sent is not in the correct format.");
        }
        userService.deleteUsersByIds(deleteRequest.getIds());
        return ApiResponse.ok(null, "Users deleted successfully");
    }

    @DeleteMapping("/disable")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Void>> disableUsers(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest.getIds() == null || deleteRequest.getIds().isEmpty()) {
            throw new IllegalArgumentException("The data sent is not in the correct format.");
        }
        userService.disableUsersByIds(deleteRequest.getIds());
        return ApiResponse.ok(null, "Users disabled successfully");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.ok(userService.getAllUsers(status, role, page, size), "Users retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_admin') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Integer id) {
        return ApiResponse.ok(userService.getUserById(id), "User retrieved successfully");
    }

    @PostMapping("/upload-profile-picture")
    public ResponseEntity<ApiResponse<Void>> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        userService.uploadProfilePicture(file);
        return ApiResponse.ok(null, "Profile picture uploaded successfully");
    }

    @GetMapping("/profile-picture")
    public ResponseEntity<ApiResponse<String>> getProfilePictureUrl() {
        return ApiResponse.ok(userService.getProfilePictureUrl(), "Profile picture URL retrieved successfully");
    }

    @GetMapping("/current-profile")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUserProfile() {
        return ApiResponse.ok(userService.getCurrentUserProfile(), "Current user profile retrieved successfully");
    }

    @PutMapping("/update-profile")
    @PreAuthorize("hasAnyAuthority('ROLE_customer', 'ROLE_organizer', 'ROLE_admin')")
    public ResponseEntity<ApiResponse<Void>> updateUserProfile(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateUserProfile(request);
        return ApiResponse.ok(null, "User profile updated successfully");
    }

    @PostMapping("/admin/approve-organizer")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Void>> approveOrganizer(@RequestParam("userId") Integer userId) {
        userService.approveOrganizer(userId);
        return ApiResponse.ok(null, "Organizer approved successfully");
    }

    @PutMapping("/admin/approve-event")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Void>> approveEvent(@RequestParam("eventId") Integer eventId) {
        userService.approveEvent(eventId);
        return ApiResponse.ok(null, "Event approved successfully");
    }

    @PutMapping("/admin/reject-event")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Void>> rejectEvent(
            @RequestParam("eventId") Integer eventId,
            @Valid @RequestBody EventRejectionReasonRequest request) {
        userService.rejectEvent(eventId, request);
        return ApiResponse.ok(null, "Event rejected successfully");
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        userService.deleteAccount();
        return ApiResponse.ok(null, "Account deleted successfully");
    }
}
