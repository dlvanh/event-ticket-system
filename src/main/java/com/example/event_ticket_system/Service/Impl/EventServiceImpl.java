package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import com.example.event_ticket_system.DTO.response.DetailEventResponseDto;
import com.example.event_ticket_system.DTO.response.GetEventsByOrganizerResponseDto;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Ticket;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Entity.Ward;
import com.example.event_ticket_system.Enums.UserRole;
import com.example.event_ticket_system.Repository.EventRepository;
import com.example.event_ticket_system.Repository.TicketRepository;
import com.example.event_ticket_system.Repository.UserRepository;
import com.example.event_ticket_system.Repository.WardRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.EventService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final WardRepository wardRepository;

    private final TicketRepository ticketRepository;

    @Autowired
    private final JwtUtil jwtUtil;

    @Value("${imgbb.api.key}")
    private String imgbbApiKey;

    @Override
    public Integer createEvent(EventRequestDto eventRequestDto,
                               MultipartFile logoFile,
                               MultipartFile backgroundFile,
                               HttpServletRequest request) {
        Integer organizerId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User currentUser = userRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Organizer not found with id: " + organizerId));

        if (!UserRole.organizer.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to create events.");
        }

        Ward ward = wardRepository.findById(eventRequestDto.getWardId())
                .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + eventRequestDto.getWardId()));

        // Kiểm tra định dạng ảnh
        List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
        if (!allowedTypes.contains(logoFile.getContentType()) || !allowedTypes.contains(backgroundFile.getContentType())) {
            throw new IllegalArgumentException("Chỉ cho phép upload file ảnh (jpg, png, gif, webp)");
        }

        try {
            // Upload logo
            String logoUrl = uploadImageToImgbb(logoFile);

            // Upload background
            String backgroundUrl = uploadImageToImgbb(backgroundFile);

            // Tạo Event
            Event event = new Event();
            event.setEventName(eventRequestDto.getEventName());
            event.setDescription(eventRequestDto.getDescription());
            event.setWard(ward);
            event.setAddressDetail(eventRequestDto.getAddressDetail());
            event.setAddressName(eventRequestDto.getAddressName());
            event.setCategory(eventRequestDto.getCategory());
            event.setStartTime(eventRequestDto.getStartTime());
            event.setEndTime(eventRequestDto.getEndTime());
            event.setOrganizer(currentUser);
            event.setCreatedAt(java.time.LocalDateTime.now());
            event.setUpdatedAt(java.time.LocalDateTime.now());
            event.setLogoUrl(logoUrl);
            event.setBackgroundUrl(backgroundUrl);
            event = eventRepository.save(event);

            // Tạo các loại vé
            for (EventRequestDto.TicketRequest t : eventRequestDto.getTickets()) {
                Ticket ticket = new Ticket();
                ticket.setEvent(event);
                ticket.setTicketType(t.getTicketType());
                ticket.setQuantityTotal(t.getQuantityTotal());
                ticket.setQuantitySold(0);
                ticket.setPrice(t.getPrice());
                ticket.setSaleStart(eventRequestDto.getSaleStart());
                ticket.setSaleEnd(eventRequestDto.getSaleEnd());
                ticketRepository.save(ticket);
            }

            return event.getEventId();
        } catch (Exception e) {
            throw new RuntimeException("Tạo sự kiện thất bại: " + e.getMessage(), e);
        }
    }
    private String uploadImageToImgbb(MultipartFile file) throws IOException, InterruptedException {
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

        return json.getJSONObject("data").getString("url");
    }

    @Override
    public Map<String, Object> getEventsByOrganizer(HttpServletRequest request,
                                                    String approveStatus,
                                                    LocalDateTime startTime,
                                                    LocalDateTime endTime,
                                                    String name,
                                                    Integer page,
                                                    Integer size) {
        Integer organizerId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User currentUser = userRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Organizer not found with id: " + organizerId));

        if (!UserRole.organizer.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to view events.");
        }

        if (page > 0) {
            page = page - 1;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Specification<Event> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("organizer").get("id"), organizerId));

            if (approveStatus != null && !approveStatus.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("approvalStatus"), approveStatus));
            }
            if (startTime != null && endTime != null) {
                // Check if event overlaps with [startTime, endTime]
                predicates.add(criteriaBuilder.and(
                        criteriaBuilder.lessThanOrEqualTo(root.get("startTime"), endTime),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endTime"), startTime)
                ));
            } else {
                if (startTime != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endTime"), startTime));
                }
                if (endTime != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startTime"), endTime));
                }
            }
            if (name != null && !name.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("eventName")), "%" + name.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Event> eventPage = eventRepository.findAll(specification, pageable);

        List<GetEventsByOrganizerResponseDto> eventDTOs = eventPage.getContent().stream().map(event -> {
            GetEventsByOrganizerResponseDto dto = new GetEventsByOrganizerResponseDto();
            dto.setEventId(event.getEventId());
            dto.setEventName(event.getEventName());
            dto.setApprovalStatus(event.getApprovalStatus().name());
            dto.setStartTime(event.getStartTime().toString());
            dto.setEndTime(event.getEndTime().toString());
            dto.setUpdateAt(event.getUpdatedAt().toString());
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("listEvents", eventDTOs);
        response.put("pageSize", eventPage.getSize());
        response.put("pageNo", eventPage.getNumber() + 1);
        response.put("totalPages", eventPage.getTotalPages());

        return response;
    }

    @Override
    public DetailEventResponseDto getEventById(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        DetailEventResponseDto responseDto = new DetailEventResponseDto();
        responseDto.setEventId(event.getEventId());
        responseDto.setEventName(event.getEventName());
        responseDto.setDescription(event.getDescription());
        responseDto.setAddress(
                (event.getAddressName() != null ? event.getAddressName() + ", " : "") +
                        (event.getAddressDetail() != null ? event.getAddressDetail() + ", " : "") +
                        event.getWard().getName() + ", " +
                        event.getWard().getDistrict().getName() + ", " +
                        event.getWard().getDistrict().getProvince().getName()
        );
        responseDto.setStartTime(event.getStartTime().toString());
        responseDto.setEndTime(event.getEndTime().toString());
        responseDto.setCategory(event.getCategory());
        responseDto.setStatus(event.getStatus().name());
        responseDto.setCreatedAt(event.getCreatedAt().toString());
        responseDto.setUpdatedAt(event.getUpdatedAt().toString());
        responseDto.setApprovalStatus(event.getApprovalStatus().name());
        responseDto.setLogoUrl(event.getLogoUrl());
        responseDto.setBackgroundUrl(event.getBackgroundUrl());

        return responseDto;
    }
}
