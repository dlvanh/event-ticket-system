package com.example.event_ticket_system.service;

import com.example.event_ticket_system.dto.request.EventRejectionReasonRequest;
import com.example.event_ticket_system.dto.request.OrganizerRequest;
import com.example.event_ticket_system.dto.request.UpdateProfileRequest;
import com.example.event_ticket_system.dto.response.UserResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService {
    void deleteUsersByIds(List<Integer> ids);
    void disableUsersByIds(List<Integer> ids);
    Map<String, Object> getAllUsers(String status, String role, Integer page, Integer size);
    UserResponseDto getUserById(Integer id);
    void uploadProfilePicture(MultipartFile file);
    String getProfilePictureUrl();
    UserResponseDto getCurrentUserProfile();
    void updateUserProfile(UpdateProfileRequest request);
    void approveOrganizer(Integer userId);
    void registerOrganizer(MultipartFile file, OrganizerRequest request);
    void approveEvent(Integer eventId);
    void rejectEvent(Integer eventId, EventRejectionReasonRequest request);
    void deleteAccount();
}
