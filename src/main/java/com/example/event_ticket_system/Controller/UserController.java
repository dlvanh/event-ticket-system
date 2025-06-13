package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.DTO.request.DeleteRequest;
import com.example.event_ticket_system.Service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    @Autowired
    private final UserService userService;

    @DeleteMapping("/delete")
    public ResponseEntity<Object> deleteUsers(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        try {
            if (deleteRequest.getIds() == null || deleteRequest.getIds().isEmpty()) {
                return APIResponse.responseBuilder(null, "The data sent is not in the correct format.", HttpStatus.BAD_REQUEST);
            }

            userService.deleteUsersByIds(deleteRequest.getIds(), request);
            return APIResponse.responseBuilder(
                    null,
                    "Users deleted successfully",
                    HttpStatus.OK
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        }catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        }catch (Exception e) {
            log.error("Unexpected error during deleting update", e);
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while deleting users",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @DeleteMapping("/disable")
    public ResponseEntity<Object> disableUsers(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        try {
            if (deleteRequest.getIds() == null || deleteRequest.getIds().isEmpty()) {
                return APIResponse.responseBuilder(null, "The data sent is not in the correct format.", HttpStatus.BAD_REQUEST);
            }

            userService.disableUsersByIds(deleteRequest.getIds(), request);
            return APIResponse.responseBuilder(
                    null,
                    "Users disabled successfully",
                    HttpStatus.OK
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (IllegalStateException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            log.error("Unexpected error during disabling users", e);
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while disabling users",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping()
    public ResponseEntity<Object> getAllUsers(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Map<String, Object> users = userService.getAllUsers(request, status, role, page, size);
            return APIResponse.responseBuilder(users, "Users retrieved successfully", HttpStatus.OK);
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (Exception e) {
            log.error("Unexpected error during retrieving users", e);
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while retrieving users",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getUserById(@PathVariable Integer id) {
        try {
            return APIResponse.responseBuilder(
                    userService.getUserById(id),
                    "User retrieved successfully",
                    HttpStatus.OK
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            log.error("Unexpected error during retrieving user by ID", e);
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while retrieving user",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/upload-profile-picture")
    public ResponseEntity<Object> uploadProfilePicture(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            userService.uploadProfilePicture(file, request);
            return APIResponse.responseBuilder(
                    null,
                    "Profile picture uploaded successfully",
                    HttpStatus.OK
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        }catch (IllegalArgumentException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("Unexpected error during profile picture upload", e);
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while uploading profile picture",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/profile-picture")
    public ResponseEntity<Object> getProfilePictureUrl(HttpServletRequest request) {
        try {
            String imageUrl = userService.getProfilePictureUrl(request);
            return APIResponse.responseBuilder(
                    imageUrl,
                    "Profile picture URL retrieved successfully",
                    HttpStatus.OK
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            log.error("Unexpected error during retrieving profile picture URL", e);
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while retrieving profile picture URL",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
