package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.dto.request.EventRejectionReasonRequest;
import com.example.event_ticket_system.dto.request.OrganizerRequest;
import com.example.event_ticket_system.dto.request.UpdateProfileRequest;
import com.example.event_ticket_system.dto.response.UserResponseDto;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.User;
import com.example.event_ticket_system.enums.ApprovalStatus;
import com.example.event_ticket_system.enums.EventStatus;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.enums.UserStatus;
import com.example.event_ticket_system.repository.EventRepository;
import com.example.event_ticket_system.repository.UserRepository;
import com.example.event_ticket_system.security.SecurityUtils;
import com.example.event_ticket_system.service.AccountService;
import com.example.event_ticket_system.service.ImageStorageService;
import com.example.event_ticket_system.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_AVATAR_URL = "https://i.ibb.co/21Lgqmdq/1c36d32072ca.png";
    private static final String VN_PHONE_REGEX =
            "^(\\+84|0)(2(0[3-9]|1[0-69]|2[025-9]|3[2-9]|4[0-9]|5[124-9]|6[039]|7[0-7]|8[0-9]|9[0-47-9])|3[2-9]|5[5689]|7[06-9]|8[0-689]|9[0-46-9])[0-9]{7}$";

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EventRepository eventRepository;
    private final ImageStorageService imageStorageService;

    @Override
    @Transactional
    public void deleteUsersByIds(List<Integer> ids) {
        requireAdmin("You do not have permission to delete user.");
        List<User> usersToDelete = userRepository.findAllById(ids);
        Set<Integer> existingIds = usersToDelete.stream().map(User::getId).collect(Collectors.toSet());
        List<Integer> notFoundIds = ids.stream().filter(id -> !existingIds.contains(id)).toList();
        if (!notFoundIds.isEmpty()) {
            throw new EntityNotFoundException("Users not found for ids: " + notFoundIds);
        }
        userRepository.deleteAll(usersToDelete);
    }

    @Override
    @Transactional
    public void disableUsersByIds(List<Integer> ids) {
        requireAdmin("You do not have permission to disable user.");
        List<User> usersToDisable = userRepository.findAllById(ids);
        Set<Integer> foundIds = usersToDisable.stream().map(User::getId).collect(Collectors.toSet());
        List<Integer> notFoundIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!notFoundIds.isEmpty()) {
            throw new EntityNotFoundException("Users not found for IDs: " + notFoundIds);
        }

        List<Integer> alreadyInactiveIds = usersToDisable.stream()
                .filter(user -> user.getStatus() == UserStatus.inactive)
                .map(User::getId)
                .toList();
        if (!alreadyInactiveIds.isEmpty()) {
            throw new IllegalStateException("The following users ids are already inactive: " + alreadyInactiveIds);
        }

        for (User user : usersToDisable) {
            user.setStatus(UserStatus.inactive);
        }
        userRepository.saveAll(usersToDisable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllUsers(String status, String role, Integer page, Integer size) {
        requireAdmin("You do not have permission to view user list.");

        int pageIndex = (page == null || page <= 1) ? 0 : page - 1;
        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Specification<User> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), UserStatus.valueOf(status.trim().toLowerCase(Locale.ROOT))));
            }
            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(root.get("role"), UserRole.valueOf(role.trim().toLowerCase(Locale.ROOT))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> pageUser = userRepository.findAll(specification, pageable);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("listUsers", pageUser.getContent().stream().map(this::convertToDTO).toList());
        response.put("pageSize", pageUser.getSize());
        response.put("pageNo", pageUser.getNumber() + 1);
        response.put("totalPage", pageUser.getTotalPages());
        response.put("totalElements", pageUser.getTotalElements());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Integer id) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.hasRole(UserRole.admin) && !currentUserId.equals(id)) {
            throw new SecurityException("You do not have permission to view this user profile.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Override
    public void uploadProfilePicture(MultipartFile file) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        String imageUrl = imageStorageService.uploadImage(file);
        user.setProfilePicture(imageUrl);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public String getProfilePictureUrl() {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return (user.getProfilePicture() == null || user.getProfilePicture().isBlank())
                ? DEFAULT_AVATAR_URL
                : user.getProfilePicture();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUserProfile() {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public void updateUserProfile(UpdateProfileRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String phoneNumber = request.getPhoneNumber().trim();
            if (!phoneNumber.matches(VN_PHONE_REGEX)) {
                throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam");
            }
            user.setPhoneNumber(phoneNumber);
        }

        user.setAddress(request.getAddress() != null && !request.getAddress().isBlank() ? request.getAddress().trim() : null);
        user.setBio(request.getBio() != null && !request.getBio().isBlank() ? request.getBio().trim() : null);
        user.setGender(request.getGender());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void approveOrganizer(Integer userId) {
        requireAdmin("You do not have permission to approve organizers.");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        user.setStatus(UserStatus.active);
        userRepository.save(user);
    }

    @Override
    public void registerOrganizer(MultipartFile file, OrganizerRequest request) {
        if (accountService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }
        if (accountService.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng.");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp.");
        }
        if (!request.getPhoneNumber().trim().matches(VN_PHONE_REGEX)) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam.");
        }

        String imageUrl = imageStorageService.uploadImage(file);

        User user = new User();
        user.setProfilePicture(imageUrl);
        user.setFullName(request.getName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setBio(request.getBio().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setStatus(UserStatus.pending);
        user.setRole(UserRole.organizer);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void approveEvent(Integer eventId) {
        requireAdmin("You do not have permission to approve events.");
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        validateEventPendingForDecision(event);
        event.setApprovalStatus(ApprovalStatus.approved);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void rejectEvent(Integer eventId, EventRejectionReasonRequest request) {
        requireAdmin("You do not have permission to reject events.");
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        validateEventPendingForDecision(event);
        event.setStatus(EventStatus.cancelled);
        event.setApprovalStatus(ApprovalStatus.rejected);
        event.setRejectionReason(request.getRejectionReason());
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void deleteAccount() {
        Integer userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        userRepository.delete(currentUser);
    }

    private void validateEventPendingForDecision(Event event) {
        if (event.getStatus() == EventStatus.completed) {
            throw new IllegalStateException("Event is already ended.");
        }
        if (event.getStatus() == EventStatus.cancelled) {
            throw new IllegalStateException("Event is already cancelled.");
        }
        if (event.getApprovalStatus() == ApprovalStatus.approved) {
            throw new IllegalStateException("Event is already approved.");
        }
        if (event.getApprovalStatus() == ApprovalStatus.rejected) {
            throw new IllegalStateException("Event is already rejected.");
        }
    }

    private void requireAdmin(String message) {
        if (!SecurityUtils.hasRole(UserRole.admin)) {
            throw new SecurityException(message);
        }
    }

    private UserResponseDto convertToDTO(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setGender(user.getGender() != null ? user.getGender().name() : null);
        dto.setAddress(user.getAddress());
        dto.setBio(user.getBio());
        dto.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        dto.setAvatarUrl(user.getProfilePicture() != null ? user.getProfilePicture() : DEFAULT_AVATAR_URL);
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        dto.setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
        return dto;
    }
}
