package com.example.event_ticket_system.service;

import com.example.event_ticket_system.dto.response.DetailEventResponseDto;
import com.example.event_ticket_system.dto.response.GetEventsByOrganizerResponseDto;
import com.example.event_ticket_system.dto.response.GetEventsResponseDto;
import com.example.event_ticket_system.dto.response.RecommendEventsResponseDto;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.Ticket;
import com.example.event_ticket_system.enums.ApprovalStatus;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.repository.EventRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.repository.TicketRepository;
import com.example.event_ticket_system.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "updatedAt", "createdAt", "startTime", "endTime", "eventName", "totalTicketSold"
    );

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final OrderTicketRepository orderTicketRepository;

    public Map<String, Object> getEventsByOrganizer(String status,
                                                    String approveStatus,
                                                    LocalDateTime startTime,
                                                    LocalDateTime endTime,
                                                    String name,
                                                    Integer page,
                                                    Integer size) {
        Integer organizerId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.hasRole(UserRole.organizer)) {
            throw new SecurityException("You do not have permission to view events.");
        }

        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Event> eventPage = eventRepository.findAll(
                EventSpecifications.byOrganizer(organizerId, status, approveStatus, startTime, endTime, name),
                pageable
        );

        List<GetEventsByOrganizerResponseDto> eventDTOs = eventPage.getContent().stream().map(event -> {
            GetEventsByOrganizerResponseDto dto = new GetEventsByOrganizerResponseDto();
            dto.setEventId(event.getEventId());
            dto.setEventName(event.getEventName());
            dto.setStatus(event.getStatus().name());
            dto.setApprovalStatus(event.getApprovalStatus().name());
            dto.setStartTime(event.getStartTime().toString());
            dto.setEndTime(event.getEndTime().toString());
            dto.setUpdateAt(event.getUpdatedAt().toString());
            dto.setLogoUrl(event.getLogoUrl());
            return dto;
        }).toList();

        return pagedResponse(eventDTOs, eventPage);
    }

    public DetailEventResponseDto getEventById(Integer eventId) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        DetailEventResponseDto dto = new DetailEventResponseDto();
        dto.setEventId(event.getEventId());
        dto.setEventName(event.getEventName());
        dto.setDescription(event.getDescription());
        dto.setAddress(formatAddress(event));
        dto.setStartTime(event.getStartTime().toString());
        dto.setEndTime(event.getEndTime().toString());
        dto.setCategory(event.getCategory());
        dto.setStatus(event.getStatus().name());
        dto.setCreatedAt(event.getCreatedAt().toString());
        dto.setUpdatedAt(event.getUpdatedAt().toString());
        dto.setApprovalStatus(event.getApprovalStatus().name());
        dto.setLogoUrl(event.getLogoUrl());
        dto.setBackgroundUrl(event.getBackgroundUrl());
        dto.setOrganizerId(event.getOrganizer().getId());
        dto.setOrganizerName(event.getOrganizer().getFullName());
        dto.setOrganizerEmail(event.getOrganizer().getEmail());
        dto.setOrganizerPhoneNumber(event.getOrganizer().getPhoneNumber());
        dto.setOrganizerBio(event.getOrganizer().getBio());
        dto.setOrganizerAvatarUrl(event.getOrganizer().getProfilePicture());

        List<Ticket> tickets = ticketRepository.findByEventEventId(eventId);

        Map<Integer, String> ticketTypes = new LinkedHashMap<>();
        Map<String, BigDecimal> ticketPrices = new LinkedHashMap<>();
        Map<String, Integer> ticketsTotal = new LinkedHashMap<>();
        Map<String, Integer> ticketsSold = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            ticketTypes.put(ticket.getTicketId(), ticket.getTicketType());
            ticketPrices.put(ticket.getTicketType(), ticket.getPrice());
            ticketsTotal.put(ticket.getTicketType(), ticket.getQuantityTotal());
            ticketsSold.put(ticket.getTicketType(), ticket.getQuantitySold());
        }
        dto.setTicketTypes(ticketTypes);
        dto.setTicketPrices(ticketPrices);
        dto.setTicketsTotal(ticketsTotal);
        dto.setTicketsSold(ticketsSold);

        dto.setTicketsSaleStartTime(tickets.stream()
                .map(Ticket::getSaleStart)
                .filter(Objects::nonNull)
                .findFirst()
                .map(LocalDateTime::toString)
                .orElse("N/A"));

        dto.setTicketsSaleEndTime(tickets.stream()
                .map(Ticket::getSaleEnd)
                .filter(Objects::nonNull)
                .findFirst()
                .map(LocalDateTime::toString)
                .orElse("N/A"));

        dto.setRejectReason(event.getRejectionReason() != null ? event.getRejectionReason() : "N/A");
        return dto;
    }

    public Map<String, Object> getRecommendEvents(String category,
                                                  String address,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  String name,
                                                  Integer page,
                                                  Integer size,
                                                  String sortBy) {
        String sortField = "updatedAt";
        String sortDirection = "DESC";
        if (sortBy != null && !sortBy.isBlank()) {
            String[] parts = sortBy.split(":");
            if (parts.length == 2 && ALLOWED_SORT_FIELDS.contains(parts[0])) {
                sortField = parts[0];
                sortDirection = parts[1];
            }
        }

        int pageIndex = normalizePage(page);
        int pageSize = normalizeSize(size);
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        if (!"totalTicketSold".equals(sortField)) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, sortField));
        }

        Page<Event> eventPage = eventRepository.findAll(
                EventSpecifications.forRecommendations(category, address, startTime, endTime, name),
                pageable
        );

        List<Event> events = eventPage.getContent();
        List<RecommendEventsResponseDto> eventDTOs = mapToRecommendDtos(events);

        if ("totalTicketSold".equals(sortField)) {
            Comparator<RecommendEventsResponseDto> comparator = Comparator.comparingLong(
                    dto -> dto.getTotalTicketSold() != null ? dto.getTotalTicketSold() : 0L);
            if (!"ASC".equalsIgnoreCase(sortDirection)) {
                comparator = comparator.reversed();
            }
            eventDTOs = new ArrayList<>(eventDTOs);
            eventDTOs.sort(comparator);
        }

        return pagedResponse(eventDTOs, eventPage);
    }

    public Map<String, Object> getPendingEvents(String address,
                                                LocalDateTime startTime,
                                                LocalDateTime endTime,
                                                String name,
                                                Integer page,
                                                Integer size) {
        if (!SecurityUtils.hasRole(UserRole.admin)) {
            throw new SecurityException("You do not have permission to view pending events.");
        }
        return getAdminEventsInternal(null, ApprovalStatus.pending.name(), address, startTime, endTime, name, page, size);
    }

    public Map<String, Object> getListEvents(String status,
                                             String approvalStatus,
                                             String address,
                                             LocalDateTime startTime,
                                             LocalDateTime endTime,
                                             String name,
                                             Integer page,
                                             Integer size) {
        if (!SecurityUtils.hasRole(UserRole.admin)) {
            throw new SecurityException("You do not have permission to view all events.");
        }
        return getAdminEventsInternal(status, approvalStatus, address, startTime, endTime, name, page, size);
    }

    public List<RecommendEventsResponseDto> mapToRecommendDtos(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Integer> eventIds = events.stream().map(Event::getEventId).toList();

        // Batch-load tickets and paid sales counts to eliminate N+1 queries
        Map<Integer, List<Ticket>> ticketsByEventId = ticketRepository.findByEventEventIdIn(eventIds)
                .stream()
                .collect(Collectors.groupingBy(t -> t.getEvent().getEventId()));

        Map<Integer, Long> soldByEventId = new HashMap<>();
        for (Object[] row : orderTicketRepository.sumPaidQuantityByEventIds(eventIds)) {
            soldByEventId.put((Integer) row[0], ((Number) row[1]).longValue());
        }

        return events.stream().map(event -> {
            RecommendEventsResponseDto dto = new RecommendEventsResponseDto();
            dto.setEventId(event.getEventId());
            dto.setEventName(event.getEventName());
            dto.setAddress(formatAddress(event));
            dto.setStartTime(event.getStartTime());
            dto.setEndTime(event.getEndTime());
            dto.setCategory(event.getCategory());
            dto.setLogoUrl(event.getLogoUrl());
            dto.setBackgroundUrl(event.getBackgroundUrl());

            List<Ticket> tickets = ticketsByEventId.getOrDefault(event.getEventId(), List.of());
            String minPrice = tickets.stream()
                    .map(Ticket::getPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .map(BigDecimal::toPlainString)
                    .orElse("0");
            dto.setMinPrice(minPrice);
            dto.setTotalTicketSold(soldByEventId.getOrDefault(event.getEventId(), 0L));
            return dto;
        }).toList();
    }

    public static String formatAddress(Event event) {
        StringBuilder sb = new StringBuilder();
        if (event.getAddressName() != null && !event.getAddressName().isBlank()) {
            sb.append(event.getAddressName()).append(", ");
        }
        if (event.getAddressDetail() != null && !event.getAddressDetail().isBlank()) {
            sb.append(event.getAddressDetail()).append(", ");
        }
        if (event.getWard() != null) {
            sb.append(event.getWard().getName());
            if (event.getWard().getDistrict() != null) {
                sb.append(", ").append(event.getWard().getDistrict().getName());
                if (event.getWard().getDistrict().getProvince() != null) {
                    sb.append(", ").append(event.getWard().getDistrict().getProvince().getName());
                }
            }
        }
        return sb.toString();
    }

    private Map<String, Object> getAdminEventsInternal(String status,
                                                       String approvalStatus,
                                                       String address,
                                                       LocalDateTime startTime,
                                                       LocalDateTime endTime,
                                                       String name,
                                                       Integer page,
                                                       Integer size) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Event> eventPage = eventRepository.findAll(
                EventSpecifications.forAdminList(status, approvalStatus, address, startTime, endTime, name),
                pageable
        );

        List<GetEventsResponseDto> eventDTOs = eventPage.getContent().stream().map(event -> {
            GetEventsResponseDto dto = new GetEventsResponseDto();
            dto.setEventId(event.getEventId());
            dto.setEventName(event.getEventName());
            dto.setStatus(event.getStatus().name());
            dto.setAddress(formatAddress(event));
            dto.setApprovalStatus(event.getApprovalStatus().name());
            dto.setStartTime(event.getStartTime().toString());
            dto.setEndTime(event.getEndTime().toString());
            dto.setUpdateAt(event.getUpdatedAt().toString());
            dto.setOrganizerName(event.getOrganizer().getFullName());
            return dto;
        }).toList();

        return pagedResponse(eventDTOs, eventPage);
    }

    private Map<String, Object> pagedResponse(List<?> items, Page<?> page) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("listEvents", items);
        response.put("pageSize", page.getSize());
        response.put("pageNo", page.getNumber() + 1);
        response.put("totalPages", page.getTotalPages());
        response.put("totalElements", page.getTotalElements());
        return response;
    }

    private int normalizePage(Integer page) {
        return (page == null || page <= 1) ? 0 : page - 1;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
