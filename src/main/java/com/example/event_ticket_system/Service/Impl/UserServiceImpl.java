package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.EventRejectionReasonRequest;
import com.example.event_ticket_system.DTO.request.OrganizerRequest;
import com.example.event_ticket_system.DTO.request.UpdateProfileRequest;
import com.example.event_ticket_system.DTO.response.UserResponseDto;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Enums.*;
import com.example.event_ticket_system.Repository.EventRepository;
import com.example.event_ticket_system.Repository.UserRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.AccountService;
import com.example.event_ticket_system.Service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Autowired
    private final JwtUtil jwtUtil;

    @Value("${imgbb.api.key}")
    private String imgbbApiKey;

    @Autowired
    private final AccountService accountService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EventRepository eventRepository;

    @Override
    public void deleteUsersByIds(List<Integer> ids, HttpServletRequest request) {
        String userRole = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        if (!"ROLE_admin".equals(userRole)) {
            throw new SecurityException("You do not have permission to delete user.");
        }
        List<User> usersToDelete = userRepository.findAllById(ids);

        List<Integer> existingIds = usersToDelete.stream()
                .map(User::getId)
                .toList();

        List<Integer> notFoundIds = ids.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        if (!notFoundIds.isEmpty()) {
            throw new EntityNotFoundException("Users not found for ids: " + notFoundIds);
        }

        userRepository.deleteAll(usersToDelete);
    }

    @Override
    public void disableUsersByIds(List<Integer> ids, HttpServletRequest request) {
        String userRole = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        if (!"ROLE_admin".equals(userRole)) {
            throw new SecurityException("You do not have permission to disable user.");
        }

        // Lấy danh sách người dùng thực tế từ DB
        List<User> usersToDisable = userRepository.findAllById(ids);

        // Tìm các ID không tồn tại
        Set<Integer> foundIds = usersToDisable.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<Integer> notFoundIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!notFoundIds.isEmpty()) {
            throw new EntityNotFoundException("Users not found for IDs: " + notFoundIds);
        }

        // Tìm các user đã bị vô hiệu hóa
        List<Integer> alreadyInactiveIds = usersToDisable.stream()
                .filter(user -> user.getStatus() == UserStatus.inactive)
                .map(User::getId)
                .toList();

        if (!alreadyInactiveIds.isEmpty()) {
            throw new IllegalStateException("The following users ids are already inactive: " + alreadyInactiveIds);
        }

        // Cập nhật status cho các user còn lại
        for (User user : usersToDisable) {
            user.setStatus(UserStatus.inactive);
        }

        userRepository.saveAll(usersToDisable);
    }


    @Override
    public Map<String, Object> getAllUsers(HttpServletRequest request, String status, String role, Integer page, Integer size) {
        String userRole = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        if (!"ROLE_admin".equals(userRole)) {
            throw new SecurityException("You do not have permission to view user list.");
        }

        if (page > 0) {
            page = page - 1;
        }

        Pageable pageable = PageRequest.of(page, size);

        Specification<User> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (role != null && !role.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> pageUser = userRepository.findAll(specification, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("listUsers", pageUser.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()));
        response.put("pageSize", pageUser.getSize());
        response.put("pageNo", pageUser.getNumber() + 1);
        response.put("totalPage", pageUser.getTotalPages());

        return response;
    }

    private UserResponseDto convertToDTO(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(String.valueOf(user.getRole()));
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setGender(String.valueOf(user.getGender()));
        dto.setAddress(user.getAddress());
        dto.setBio(user.getBio());
        dto.setStatus(String.valueOf(user.getStatus()));
        dto.setAvatarUrl(user.getProfilePicture() != null ? user.getProfilePicture() : "https://i.ibb.co/21Lgqmdq/1c36d32072ca.png");
        dto.setCreatedAt(user.getCreatedAt().toString());
        dto.setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
        return dto;
    }

    @Override
    public UserResponseDto getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Override
    public void uploadProfilePicture(MultipartFile file, HttpServletRequest request) {
        User user = userRepository.findById(jwtUtil.extractUserId(request.getHeader("Authorization").substring(7)))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
        if (!allowedTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ cho phép upload file ảnh (jpg, png, gif, webp)");
        }
        try {
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String url = "https://api.imgbb.com/1/upload?key=" + imgbbApiKey;

            HttpClient client = HttpClient.newHttpClient();
            String body = "image=" + URLEncoder.encode(base64Image, StandardCharsets.UTF_8);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            String imageUrl = json.getJSONObject("data").getString("url");

            //Gán image URL vào User
            user.setProfilePicture(imageUrl);
            userRepository.save(user);

            System.out.println("Uploaded image URL: " + imageUrl);

        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    @Override
    public String getProfilePictureUrl(HttpServletRequest request) {
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getProfilePicture() == null || user.getProfilePicture().isEmpty()) {
            return "https://i.ibb.co/21Lgqmdq/1c36d32072ca.png";
        }

        return user.getProfilePicture();
    }

    @Override
    public UserResponseDto getCurrentUserProfile(HttpServletRequest request) {
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return convertToDTO(user);
    }

    @Override
    public void updateUserProfile(UpdateProfileRequest request, HttpServletRequest httpServletRequest) {
        Integer userId = jwtUtil.extractUserId(httpServletRequest.getHeader("Authorization").substring(7));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        if (request.getPhoneNumber() != null) {
            String phoneNumber = request.getPhoneNumber().trim();
            String vnPhoneRegex = "^(\\+84|0)(2(0[3-9]|1[0-69]|2[025-9]|3[2-9]|4[0-9]|5[124-9]|6[039]|7[0-7]|8[0-9]|9[0-47-9])|3[2-9]|5[5689]|7[06-9]|8[0-689]|9[0-46-9])[0-9]{7}$";

            if (!phoneNumber.isBlank() && phoneNumber.matches(vnPhoneRegex)) {
                user.setPhoneNumber(phoneNumber);
            } else {
                throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam");
            }
        }

        user.setAddress(request.getAddress() != null && !request.getAddress().isEmpty() ? request.getAddress() : null);

        user.setBio(request.getBio() != null && !request.getBio().isEmpty() ? request.getBio() : null);

        if (request.getGender() != null && !request.getGender().isEmpty()) {
            try {
                Gender gender = Gender.valueOf(String.valueOf(request.getGender()));
                user.setGender(gender);
            } catch (IllegalArgumentException e) { //? chưa biết why ko dc
                throw new IllegalArgumentException("Phải là 1 trong 3 giá trị này: Male, Female, Other");
            }
        } else {
            user.setGender(null);
        }

        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    public void approveOrganizer(Integer userId, HttpServletRequest request) {
        String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        if (!"ROLE_admin".equals(role)) {
            throw new SecurityException("You do not have permission to approve organizers.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getRole() == UserRole.organizer) {
            throw new IllegalStateException("User is already an organizer.");
        }

        user.setStatus(UserStatus.active);
        user.setRole(UserRole.organizer);
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

        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{10,}$";
        String vnPhoneRegex = "^(\\+84|0)(2(0[3-9]|1[0-69]|2[025-9]|3[2-9]|4[0-9]|5[124-9]|6[039]|7[0-7]|8[0-9]|9[0-47-9])|3[2-9]|5[5689]|7[06-9]|8[0-689]|9[0-46-9])[0-9]{7}$";

        if (request.getPassword().isBlank() || request.getConfirmPassword().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }

        if (!request.getPassword().matches(passwordRegex) || !request.getConfirmPassword().matches(passwordRegex)) {
            throw new IllegalArgumentException
                    ("Mật khẩu phải có ít nhất 10 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        if (request.getPassword().matches(passwordRegex) && !request.getConfirmPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp.");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }

        if (!request.getPhoneNumber().matches(vnPhoneRegex)) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam.");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên không được để trống.");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ảnh đại diện không được để trống.");
        }

        User user = new User();

        List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
        if (!allowedTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ cho phép upload file ảnh (jpg, png, gif, webp)");
        }
        try {
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String url = "https://api.imgbb.com/1/upload?key=" + imgbbApiKey;

            HttpClient client = HttpClient.newHttpClient();
            String body = "image=" + URLEncoder.encode(base64Image, StandardCharsets.UTF_8);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            String imageUrl = json.getJSONObject("data").getString("url");

            //Gán image URL vào User
            user.setProfilePicture(imageUrl);

            System.out.println("Uploaded image URL: " + imageUrl);

        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
        user.setFullName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setBio(request.getBio());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(UserStatus.pending);
        user.setRole(UserRole.customer);
        user.setCreatedAt(LocalDateTime.now().toInstant(java.time.ZoneOffset.UTC));
        userRepository.save(user);

    }

    @Override
    public void approveEvent(Integer eventId, HttpServletRequest request) {
        String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        if (!"ROLE_admin".equals(role)) {
            throw new SecurityException("You do not have permission to approve events.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        //TODO: refine this
        if (event.getStatus().equals(EventStatus.completed)) {
            throw new IllegalStateException("Event is already ended.");
        }

        if (event.getStatus().equals(EventStatus.cancelled)) {
            throw new IllegalStateException("Event is already cancelled.");
        }

        if (event.getApprovalStatus().equals(ApprovalStatus.approved)) {
            throw new IllegalStateException("Event is already approved.");
        }

        if (event.getApprovalStatus().equals(ApprovalStatus.rejected)) {
            throw new IllegalStateException("Event is already rejected.");
        }

        if (event.getApprovalStatus().equals(ApprovalStatus.pending)) {
            event.setApprovalStatus(ApprovalStatus.approved);
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);
        } else {
            throw new RuntimeException("Approval failed.");
        }
    }

    @Override
    public void rejectEvent(Integer eventId, HttpServletRequest request, EventRejectionReasonRequest eventRejectionReasonRequest) {
        String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        if (!"ROLE_admin".equals(role)) {
            throw new SecurityException("You do not have permission to reject events.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        if (event.getStatus().equals(EventStatus.completed)) {
            throw new IllegalStateException("Event is already ended.");
        }

        if (event.getStatus().equals(EventStatus.cancelled)) {
            throw new IllegalStateException("Event is already cancelled.");
        }

        if (event.getApprovalStatus().equals(ApprovalStatus.approved)) {
            throw new IllegalStateException("Event is already approved.");
        }

        if (event.getApprovalStatus().equals(ApprovalStatus.rejected)) {
            throw new IllegalStateException("Event is already rejected.");
        }

        if (event.getApprovalStatus().equals(ApprovalStatus.pending)) {
            event.setStatus(EventStatus.cancelled);
            event.setApprovalStatus(ApprovalStatus.rejected);
            event.setRejectionReason(eventRejectionReasonRequest.getRejectionReason());
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);
        } else {
            throw new RuntimeException("Rejection failed.");
        }
    }
}
