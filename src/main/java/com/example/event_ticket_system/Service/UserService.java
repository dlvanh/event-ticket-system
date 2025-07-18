package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.request.EventRejectionReasonRequest;
import com.example.event_ticket_system.DTO.request.OrganizerRequest;
import com.example.event_ticket_system.DTO.request.UpdateProfileRequest;
import com.example.event_ticket_system.DTO.response.UserResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService {
    UserResponseDto getUserById(Integer id);
    UserResponseDto getCurrentUserProfile(HttpServletRequest request);
    void uploadProfilePicture(MultipartFile file, HttpServletRequest request);
    String getProfilePictureUrl(HttpServletRequest request);
    void deleteUsersByIds(List<Integer> ids, HttpServletRequest request);
    void disableUsersByIds(List<Integer> ids, HttpServletRequest request);
    Map<String, Object> getAllUsers(HttpServletRequest request, String status, String role, Integer page, Integer size);
    void updateUserProfile(UpdateProfileRequest request, HttpServletRequest httpServletRequest);
    void approveOrganizer(Integer userId, HttpServletRequest request);
    void registerOrganizer(MultipartFile file, OrganizerRequest request);
    void approveEvent(Integer eventId, HttpServletRequest request);
    void rejectEvent(Integer eventId, HttpServletRequest request, EventRejectionReasonRequest eventRejectionReasonRequest);
    void deleteAccount(HttpServletRequest request);
}
