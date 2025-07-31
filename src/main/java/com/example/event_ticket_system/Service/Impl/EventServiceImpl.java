package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import com.example.event_ticket_system.DTO.request.UpdateEventRequestDto;
import com.example.event_ticket_system.DTO.response.*;
import com.example.event_ticket_system.Entity.*;
import com.example.event_ticket_system.Enums.ApprovalStatus;
import com.example.event_ticket_system.Enums.DiscountType;
import com.example.event_ticket_system.Enums.EventStatus;
import com.example.event_ticket_system.Enums.UserRole;
import com.example.event_ticket_system.Repository.*;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.EventService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final WardRepository wardRepository;

    private final TicketRepository ticketRepository;

    private final OrderTicketRepository orderTicketRepository;

    private final DiscountRepository discountRepository;

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

            // Tạo các mã giảm giá nếu có
            if (eventRequestDto.getDiscounts() != null) {
                for (EventRequestDto.DiscountRequest discount : eventRequestDto.getDiscounts()) {
                    Discount newDiscount = new Discount();
                    newDiscount.setEvent(event);
                    newDiscount.setCode(discount.getDiscountCode());
                    newDiscount.setDescription(discount.getDiscountDescription());
                    newDiscount.setDiscountType(DiscountType.valueOf(discount.getDiscountType()));
                    newDiscount.setValue(discount.getDiscountValue());
                    newDiscount.setValidFrom(discount.getDiscountValidFrom());
                    newDiscount.setValidTo(discount.getDiscountValidTo());
                    newDiscount.setMaxUsage(discount.getDiscountMaxUses());
                    discountRepository.save(newDiscount);
                }
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
            dto.setStatus(event.getStatus().name());
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

        Map<Integer, String> ticketTypes = new HashMap<>();
        for (Ticket ticket : tickets) {
            ticketTypes.put(ticket.getTicketId(), ticket.getTicketType());
        }
        responseDto.setTicketTypes(ticketTypes);

        Map<String, Double> ticketPrices = new HashMap<>();
        for (Ticket ticket : tickets) {
            ticketPrices.put(ticket.getTicketType(), ticket.getPrice());
        }
        responseDto.setTicketPrices(ticketPrices);

        Map<String, Integer> ticketsTotal = new HashMap<>();
        for (Ticket ticket : tickets) {
            ticketsTotal.put(ticket.getTicketType(), ticket.getQuantityTotal());
        }
        responseDto.setTicketsTotal(ticketsTotal);

        Map<String, Integer> ticketSoldMap = new HashMap<>();
        for (Ticket ticket : tickets) {
            ticketSoldMap.put(ticket.getTicketType(), ticket.getQuantitySold());
        }
        responseDto.setTicketsSold(ticketSoldMap);

        responseDto.setTicketsSaleStartTime(tickets.stream()
                .map(Ticket::getSaleStart)
                .filter(Objects::nonNull)
                .findFirst()
                .map(LocalDateTime::toString)
                .orElse("N/A"));

        responseDto.setTicketsSaleEndTime(tickets.stream()
                .map(Ticket::getSaleEnd)
                .filter(Objects::nonNull)
                .findFirst()
                .map(LocalDateTime::toString)
                .orElse("N/A"));

        responseDto.setRejectReason(event.getRejectionReason() != null ? event.getRejectionReason() : "N/A");
        return responseDto;
    }

    @Override
    public Map<String, Object> getRecommendEvents(
            String category, String address, LocalDateTime startTime, LocalDateTime endTime,
            String name, Integer page, Integer size, String sortBy) {

        if (page > 0) {
            page = page - 1;
        }

        // Default sort
        String sortField = "updatedAt";
        String sortDirection = "DESC";

        if (sortBy != null && !sortBy.isEmpty()) {
            String[] parts = sortBy.split(":");
            if (parts.length == 2) {
                sortField = parts[0];
                sortDirection = parts[1];
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        if (!"totalTicketSold".equals(sortField)) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        }

        Specification<Event> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("approvalStatus"), ApprovalStatus.approved));
            CriteriaBuilder.In<EventStatus> statusIn = criteriaBuilder.in(root.get("status"));
            statusIn.value(EventStatus.completed);
            statusIn.value(EventStatus.upcoming);
            predicates.add(statusIn);

            if (category != null && !category.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (address != null && !address.isEmpty()) {
                String pattern = "%" + address.toLowerCase() + "%";
                Predicate addressNameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("addressName")), pattern);
                Predicate addressDetailLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("addressDetail")), pattern);
                Predicate wardLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("ward").get("name")), pattern);
                Predicate districtLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("ward").get("district").get("name")), pattern);
                Predicate provinceLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("ward").get("district").get("province").get("name")), pattern);

                predicates.add(criteriaBuilder.or(addressNameLike, addressDetailLike, wardLike, districtLike, provinceLike));
            }
            if (startTime != null && endTime != null) {
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
            dto.setStartTime(LocalDateTime.parse(event.getStartTime().toString()));
            dto.setEndTime(LocalDateTime.parse(event.getEndTime().toString()));
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
            dto.setTotalTicketSold(orderTicketRepository.sumQuantityByEvent(event));
            return dto;
        }).collect(Collectors.toList());

        // If sorting by totalTicketSold, sort in Java
        if ("totalTicketSold".equals(sortField)) {
            if ("ASC".equalsIgnoreCase(sortDirection)) {
                eventDTOs.sort(Comparator.comparingLong(RecommendEventsResponseDto::getTotalTicketSold));
            } else {
                eventDTOs.sort(Comparator.comparingLong(RecommendEventsResponseDto::getTotalTicketSold).reversed());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("listEvents", eventDTOs);
        response.put("pageSize", eventPage.getSize());
        response.put("pageNo", eventPage.getNumber() + 1);
        response.put("totalPages", eventPage.getTotalPages());
        return response;
    }

    @Override
    public Map<String, Object> getPendingEvents(HttpServletRequest request, String address, LocalDateTime startTime, LocalDateTime endTime, String name, Integer page, Integer size) {
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

    @Override
    public void updateEvent(Integer eventId, UpdateEventRequestDto eventRequestDto, MultipartFile logoFile, MultipartFile backgroundFile, HttpServletRequest request) {
        Integer organizerId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User currentUser = userRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Organizer not found with id: " + organizerId));

        if (!UserRole.organizer.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to update events.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new SecurityException("You do not have permission to update this event.");
        }

        if (eventRequestDto.getStartTime().isAfter(eventRequestDto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        // Kiểm tra định dạng ảnh
        List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
        if (logoFile != null && !allowedTypes.contains(logoFile.getContentType())) {
            throw new IllegalArgumentException("Chỉ cho phép upload file ảnh (jpg, png, gif, webp)");
        }
        if (backgroundFile != null && !allowedTypes.contains(backgroundFile.getContentType())) {
            throw new IllegalArgumentException("Chỉ cho phép upload file ảnh (jpg, png, gif, webp)");
        }

        try {
            if (logoFile != null) {
                String logoUrl = uploadImageToImgbb(logoFile);
                event.setLogoUrl(logoUrl);
            }
            if (backgroundFile != null) {
                String backgroundUrl = uploadImageToImgbb(backgroundFile);
                event.setBackgroundUrl(backgroundUrl);
            }

            // Cập nhật thông tin sự kiện
            event.setEventName(eventRequestDto.getEventName());
            event.setDescription(eventRequestDto.getDescription());
            event.setAddressName(eventRequestDto.getAddressName());
            event.setAddressDetail(eventRequestDto.getAddressDetail());
            Ward ward = wardRepository.findById(eventRequestDto.getWardId())
                    .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + eventRequestDto.getWardId()));
            event.setWard(ward);
            event.setStartTime(eventRequestDto.getStartTime());
            event.setEndTime(eventRequestDto.getEndTime());
            event.setCategory(eventRequestDto.getCategory());
            event.setUpdatedAt(LocalDateTime.now());

            // Reset approval status
            event.setApprovalStatus(ApprovalStatus.pending);

            eventRepository.save(event);
            // Xử lý tickets
            List<UpdateEventRequestDto.TicketTypeDto> ticketDtos = eventRequestDto.getTicketTypes();
            if (ticketDtos != null) {
                // Tạo set các ticketType gửi lên
                Set<String> incomingTicketTypes = ticketDtos.stream()
                        .map(UpdateEventRequestDto.TicketTypeDto::getTicketType)
                        .collect(Collectors.toSet());

                // Lấy tất cả ticket hiện có
                List<Ticket> existingTickets = ticketRepository.findByEvent(event);

                // Xóa ticket không còn trong payload
                for (Ticket existing : existingTickets) {
                    if (!incomingTicketTypes.contains(existing.getTicketType())) {
                        ticketRepository.delete(existing);
                    }
                }

                // Cập nhật hoặc thêm mới
                for (UpdateEventRequestDto.TicketTypeDto dto : ticketDtos) {
                    // tìm xem ticket này đã tồn tại chưa
                    Optional<Ticket> existingOpt = existingTickets.stream()
                            .filter(t -> t.getTicketType().equals(dto.getTicketType()))
                            .findFirst();

                    if (existingOpt.isPresent()) {
                        // Update
                        Ticket ticket = existingOpt.get();
                        ticket.setQuantityTotal(dto.getQuantityTotal());
                        ticket.setPrice(dto.getPrice());
                        ticketRepository.save(ticket);
                    } else {
                        // Thêm mới
                        Ticket newTicket = new Ticket();
                        newTicket.setTicketType(dto.getTicketType());
                        newTicket.setQuantityTotal(dto.getQuantityTotal());
                        newTicket.setPrice(dto.getPrice());
                        newTicket.setQuantitySold(0); // Mặc định số lượng đã bán là 0
                        newTicket.setEvent(event);
                        ticketRepository.save(newTicket);
                    }
                }
            }

            // Xử lý mã giảm giá
            List<UpdateEventRequestDto.DiscountDto> discountDtos = eventRequestDto.getDiscounts();
            if (discountDtos != null) {
                // Tạo set các mã giảm giá gửi lên
                Set<String> incomingDiscountCodes = discountDtos.stream()
                        .map(UpdateEventRequestDto.DiscountDto::getCode)
                        .collect(Collectors.toSet());

                // Lấy tất cả mã giảm giá hiện có
                List<Discount> existingDiscounts = discountRepository.findByEvent(event);

                // Xóa mã giảm giá không còn trong payload
                for (Discount existing : existingDiscounts) {
                    if (!incomingDiscountCodes.contains(existing.getCode())) {
                        discountRepository.delete(existing);
                    }
                }

                // Cập nhật hoặc thêm mới mã giảm giá
                for (UpdateEventRequestDto.DiscountDto dto : discountDtos) {
                    Optional<Discount> existingOpt = existingDiscounts.stream()
                            .filter(d -> d.getCode().equals(dto.getCode()))
                            .findFirst();

                    if (existingOpt.isPresent()) {
                        // Update
                        Discount discount = existingOpt.get();
                        discount.setDescription(dto.getDescription());
                        discount.setDiscountType(DiscountType.valueOf(dto.getType()));
                        discount.setValue(dto.getValue());
                        discount.setValidFrom(dto.getValidFrom());
                        discount.setValidTo(dto.getValidTo());
                        discount.setMaxUsage(dto.getMaxUses());
                        discountRepository.save(discount);
                    } else {
                        // Thêm mới
                        Discount newDiscount = new Discount();
                        newDiscount.setEvent(event);
                        newDiscount.setCode(dto.getCode());
                        newDiscount.setDescription(dto.getDescription());
                        newDiscount.setDiscountType(DiscountType.valueOf(dto.getType()));
                        newDiscount.setValue(dto.getValue());
                        newDiscount.setValidFrom(dto.getValidFrom());
                        newDiscount.setValidTo(dto.getValidTo());
                        newDiscount.setMaxUsage(dto.getMaxUses());
                        discountRepository.save(newDiscount);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Cập nhật sự kiện thất bại: " + e.getMessage(), e);
        }
    }

    public List<TicketExportDto> getTicketStatsByEvent(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        List<Ticket> tickets = ticketRepository.findByEvent(event);

        List<TicketExportDto> dtos = new ArrayList<>();
        for (Ticket ticket : tickets) {
            int total = ticket.getQuantityTotal();
            int sold = orderTicketRepository.sumQuantityByTicket(ticket);
            int remaining = total - sold;
            double revenue = sold * ticket.getPrice();

            dtos.add(new TicketExportDto(
                    ticket.getTicketType(), total, sold, remaining, revenue
            ));
        }

        return dtos;
    }

    @Override
    public byte[] generateExcelReport(HttpServletRequest request, Integer eventId) {
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        if (!UserRole.admin.equals(currentUser.getRole()) && !UserRole.organizer.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to generate reports.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getOrganizer().getId().equals(userId)&& !UserRole.admin.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to access this event.");
        }
        try {
            List<TicketExportDto> dtos = getTicketStatsByEvent(eventId);
            ByteArrayInputStream in = exportToExcel(dtos);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private ByteArrayInputStream exportToExcel(List<TicketExportDto> dtos) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Ticket Stats");

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Ticket Type", "Total Quantity", "Sold Quantity", "Remaining Quantity", "Revenue"};

        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Data rows + calculate total revenue
        int rowIndex = 1;
        double totalRevenue = 0;
        for (TicketExportDto dto : dtos) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(dto.getTicketType());
            row.createCell(1).setCellValue(dto.getTotalQuantity());
            row.createCell(2).setCellValue(dto.getSoldQuantity());
            row.createCell(3).setCellValue(dto.getRemainingQuantity());
            row.createCell(4).setCellValue(dto.getRevenue());
            totalRevenue += dto.getRevenue();
        }

        // Add total revenue row
        Row totalRow = sheet.createRow(rowIndex);
        totalRow.createCell(3).setCellValue("Total Revenue");
        totalRow.createCell(4).setCellValue(totalRevenue);

        // Auto-size all columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public byte[] generatePdfReport(HttpServletRequest request, Integer eventId) {
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        if (!UserRole.admin.equals(currentUser.getRole()) && !UserRole.organizer.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to generate reports.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getOrganizer().getId().equals(userId)&& !UserRole.admin.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to access this event.");
        }

        try {
            List<TicketExportDto> dtos = getTicketStatsByEvent(eventId);
            return exportToPdf(dtos).readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
    }

    private Font loadVietnameseFont(float size, boolean bold) throws IOException, DocumentException {
        String fontPath = "src/main/resources/fonts/arial.ttf";
        BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return new Font(baseFont, size, bold ? Font.BOLD : Font.NORMAL);
    }

    private ByteArrayInputStream exportToPdf(List<TicketExportDto> dtos) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Load font hỗ trợ tiếng Việt
        Font fontTitle = loadVietnameseFont(16f, true);
        Font fontHeader = loadVietnameseFont(12f, true);
        Font fontBody = loadVietnameseFont(12f, false);

        // Tiêu đề
        Paragraph title = new Paragraph("Thống kê vé sự kiện", fontTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);
        document.add(title);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 2, 2, 2, 2});

        // Header row
        Stream.of("Loại vé", "Tổng vé", "Đã bán", "Còn lại", "Doanh thu")
                .forEach(h -> {
                    PdfPCell header = new PdfPCell(new Phrase(h, fontHeader));
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    table.addCell(header);
                });

        double totalRevenue = 0;
        for (TicketExportDto dto : dtos) {
            table.addCell(new PdfPCell(new Phrase(dto.getTicketType(), fontBody)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(dto.getTotalQuantity()), fontBody)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(dto.getSoldQuantity()), fontBody)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(dto.getRemainingQuantity()), fontBody)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(dto.getRevenue()), fontBody)));
            totalRevenue += dto.getRevenue();
        }

        // Tổng doanh thu row
        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setColspan(3);
        empty.setBorder(Rectangle.NO_BORDER);
        table.addCell(empty);

        table.addCell(new PdfPCell(new Phrase("Tổng doanh thu", fontHeader)));
        table.addCell(new PdfPCell(new Phrase(String.valueOf(totalRevenue), fontHeader)));

        document.add(table);
        document.close();

        return new ByteArrayInputStream(out.toByteArray());
    }

}
