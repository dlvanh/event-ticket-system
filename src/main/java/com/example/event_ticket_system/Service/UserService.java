package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.response.UserResponseDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface UserService {
    UserResponseDto getUserById(Integer id);
    void deleteUsersByIds(List<Integer> ids, HttpServletRequest request);
    void disableUsersByIds(List<Integer> ids, HttpServletRequest request);
    Map<String,Object> getAllUsers(HttpServletRequest request, String status, String role, Integer page, Integer size);
}
