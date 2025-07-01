package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import com.example.event_ticket_system.DTO.response.DetailEventResponseDto;
import com.example.event_ticket_system.DTO.response.GetEventsByOrganizerResponseDto;
import com.example.event_ticket_system.DTO.response.GetEventsResponseDto;
import com.example.event_ticket_system.DTO.response.RecommendEventsResponseDto;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Ticket;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Entity.Ward;
import com.example.event_ticket_system.Enums.ApprovalStatus;
import com.example.event_ticket_system.Enums.EventStatus;
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
                                                    String status,
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

            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
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
    public DetailEventResponseDto getEventById(Integer eventId, HttpServletRequest request) {
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
        responseDto.setOrganizerId(event.getOrganizer().getId());
        responseDto.setOrganizerName(event.getOrganizer().getFullName());
        responseDto.setOrganizerEmail(event.getOrganizer().getEmail());
        responseDto.setOrganizerBio(event.getOrganizer().getBio());
        responseDto.setOrganizerAvatarUrl(event.getOrganizer().getProfilePicture());

        List<Ticket> tickets = ticketRepository.findByEvent(event);
        Map<String, Double> ticketPrices = new HashMap<>();
        for (Ticket ticket : tickets) {
            ticketPrices.put(ticket.getTicketType(), ticket.getPrice());
        }
        responseDto.setTicketPrices(ticketPrices);

        // Check if Authorization header exists
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Integer userId = jwtUtil.extractUserId(authHeader.substring(7));
                User currentUser = userRepository.findById(userId)
                        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
                if (!UserRole.customer.equals(currentUser.getRole())) {
                    Map<String, Integer> ticketSoldMap = new HashMap<>();
                    for (Ticket ticket : tickets) {
                        ticketSoldMap.put(ticket.getTicketType(), ticket.getQuantitySold());
                    }
                    responseDto.setTicketsSold(ticketSoldMap);
                }
            } catch (Exception e) {
                // log.warn("Invalid token or user not found", e);
                // Do nothing, just skip showing ticketsSold
            }
        }
        // if no Authorization header -> no ticketsSold
        responseDto.setRejectReason( event.getRejectionReason() != null ? event.getRejectionReason() : "N/A");
        return responseDto;
    }


    @Override
    public Map<String, Object> getRecommendEvents(String category, String address, LocalDateTime startTime, LocalDateTime endTime, String name, Integer page, Integer size) {
        if (page > 0) {
            page = page - 1;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Specification<Event> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Approval status must be APPROVED
            predicates.add(criteriaBuilder.equal(root.get("approvalStatus"), ApprovalStatus.approved));

            // Event status must be UPCOMING
            predicates.add(criteriaBuilder.equal(root.get("status"), EventStatus.upcoming));

            if (category != null && !category.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (address != null && !address.isEmpty()) {
                String pattern = "%" + address.toLowerCase() + "%";

                Predicate addressNameLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("addressName")), pattern);

                Predicate addressDetailLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("addressDetail")), pattern);

                Predicate wardLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ward").get("name")), pattern);

                Predicate districtLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ward").get("district").get("name")), pattern);

                Predicate provinceLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ward").get("district").get("province").get("name")), pattern);

                predicates.add(criteriaBuilder.or(addressNameLike, addressDetailLike, wardLike, districtLike, provinceLike));
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

        List<RecommendEventsResponseDto> eventDTOs = eventPage.getContent().stream().map(event -> {
            RecommendEventsResponseDto dto = new RecommendEventsResponseDto();
            dto.setEventId(event.getEventId());
            dto.setEventName(event.getEventName());
            dto.setAddress(
                    (event.getAddressName() != null ? event.getAddressName() + ", " : "") +
                            (event.getAddressDetail() != null ? event.getAddressDetail() + ", " : "") +
                            event.getWard().getName() + ", " +
                            event.getWard().getDistrict().getName() + ", " +
                            event.getWard().getDistrict().getProvince().getName()
            );
            dto.setStartTime(event.getStartTime().toString());
            dto.setEndTime(event.getEndTime().toString());
            dto.setCategory(event.getCategory());
            dto.setLogoUrl(event.getLogoUrl());
            List<Ticket> tickets = ticketRepository.findByEvent(event);
            String minPrice = tickets.stream()
                    .map(Ticket::getPrice)
                    .min(Double::compareTo)
                    .map(String::valueOf)
                    .orElse("0.0");
            dto.setMinPrice(minPrice);
            dto.setBackgroundUrl(event.getBackgroundUrl());
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
    public Map<String, Object> getPendingEvents(HttpServletRequest request,String address, LocalDateTime startTime, LocalDateTime endTime, String name,  Integer page, Integer size) {
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (!UserRole.admin.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to view pending events.");
        }
        if (page > 0) {
            page = page - 1;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Specification<Event> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (address != null && !address.isEmpty()) {
                String pattern = "%" + address.toLowerCase() + "%";

                Predicate addressNameLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("addressName")), pattern);

                Predicate addressDetailLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("addressDetail")), pattern);

                Predicate wardLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ward").get("name")), pattern);

                Predicate districtLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ward").get("district").get("name")), pattern);

                Predicate provinceLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ward").get("district").get("province").get("name")), pattern);

                predicates.add(criteriaBuilder.or(addressNameLike, addressDetailLike, wardLike, districtLike, provinceLike));
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
            predicates.add(criteriaBuilder.equal(root.get("approvalStatus"), ApprovalStatus.pending));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Event> eventPage = eventRepository.findAll(specification, pageable);

        List<GetEventsResponseDto> eventDTOs = eventPage.getContent().stream().map(event -> {
            GetEventsResponseDto dto = new GetEventsResponseDto();
            dto.setEventId(event.getEventId());
            dto.setEventName(event.getEventName());
            dto.setStatus(event.getStatus().name());
            dto.setApprovalStatus(event.getApprovalStatus().name());
            dto.setStartTime(event.getStartTime().toString());
            dto.setEndTime(event.getEndTime().toString());
            dto.setUpdateAt(event.getUpdatedAt().toString());
            dto.setOrganizerName(event.getOrganizer().getFullName());
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
    public Map<String, Object> getListEvents(HttpServletRequest request, String status, String approvalStatus, String address, LocalDateTime startTime, LocalDateTime endTime, String name, Integer page, Integer size) {
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (!UserRole.admin.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to view all events.");
        }

        if (page > 0) {
            page = page - 1;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Specification<Event> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (address != null && !address.isEmpty()) {
                String pattern = "%" + address.toLowerCase() + "%";

                Predicate addressNameLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("addressName")), pattern);

                Predicate addressDetailLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("addressDetail")), pattern);

                Predicate wardLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ward").get("name")), pattern);

                Predicate districtLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ward").get("district").get("name")), pattern);

                Predicate provinceLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ward").get("district").get("province").get("name")), pattern);

                predicates.add(criteriaBuilder.or(addressNameLike, addressDetailLike, wardLike, districtLike, provinceLike));
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
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("eventName")), "%" + name.toLowerCase() + "%"));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (approvalStatus != null && !approvalStatus.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("approvalStatus"), approvalStatus));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Event> eventPage = eventRepository.findAll(specification, pageable);
        List<GetEventsResponseDto> eventDTOs = eventPage.getContent().stream().map(event -> {
            GetEventsResponseDto dto = new GetEventsResponseDto();
            dto.setEventId(event.getEventId());
            dto.setEventName(event.getEventName());
            dto.setStatus(event.getStatus().name());
            dto.setApprovalStatus(event.getApprovalStatus().name());
            dto.setStartTime(event.getStartTime().toString());
            dto.setEndTime(event.getEndTime().toString());
            dto.setUpdateAt(event.getUpdatedAt().toString());
            dto.setOrganizerName(event.getOrganizer().getFullName());
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("listEvents", eventDTOs);
        response.put("pageSize", eventPage.getSize());
        response.put("pageNo", eventPage.getNumber() + 1);
        response.put("totalPages", eventPage.getTotalPages());
        return response;
    }
}
