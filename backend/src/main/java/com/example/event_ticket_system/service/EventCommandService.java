package com.example.event_ticket_system.service;

import com.example.event_ticket_system.dto.request.EventRequestDto;
import com.example.event_ticket_system.dto.request.UpdateEventRequestDto;
import com.example.event_ticket_system.entity.Discount;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.Ticket;
import com.example.event_ticket_system.entity.User;
import com.example.event_ticket_system.entity.Ward;
import com.example.event_ticket_system.enums.ApprovalStatus;
import com.example.event_ticket_system.enums.DiscountType;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.repository.DiscountRepository;
import com.example.event_ticket_system.repository.EventRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.repository.TicketRepository;
import com.example.event_ticket_system.repository.UserRepository;
import com.example.event_ticket_system.repository.WardRepository;
import com.example.event_ticket_system.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventCommandService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final WardRepository wardRepository;
    private final TicketRepository ticketRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final DiscountRepository discountRepository;
    private final ImageStorageService imageStorageService;
    private final TransactionTemplate transactionTemplate;

    public Integer createEvent(EventRequestDto dto, MultipartFile logoFile, MultipartFile backgroundFile) {
        Integer organizerId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.hasRole(UserRole.organizer)) {
            throw new SecurityException("You do not have permission to create events.");
        }
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
        if (dto.getSaleStart().isAfter(dto.getSaleEnd())) {
            throw new IllegalArgumentException("Ticket sale start time must be before sale end time.");
        }

        imageStorageService.validateImageFile(logoFile);
        imageStorageService.validateImageFile(backgroundFile);

        // Perform external HTTP uploads outside the DB transaction
        String logoUrl = imageStorageService.uploadImage(logoFile);
        String backgroundUrl = imageStorageService.uploadImage(backgroundFile);

        return transactionTemplate.execute(status -> {
            User organizer = userRepository.findById(organizerId)
                    .orElseThrow(() -> new EntityNotFoundException("Organizer not found with id: " + organizerId));
            Ward ward = wardRepository.findById(dto.getWardId())
                    .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + dto.getWardId()));

            LocalDateTime now = LocalDateTime.now();
            Event event = new Event();
            event.setEventName(dto.getEventName());
            event.setDescription(dto.getDescription());
            event.setWard(ward);
            event.setAddressDetail(dto.getAddressDetail());
            event.setAddressName(dto.getAddressName());
            event.setCategory(dto.getCategory());
            event.setStartTime(dto.getStartTime());
            event.setEndTime(dto.getEndTime());
            event.setOrganizer(organizer);
            event.setCreatedAt(now);
            event.setUpdatedAt(now);
            event.setLogoUrl(logoUrl);
            event.setBackgroundUrl(backgroundUrl);
            event = eventRepository.save(event);

            for (EventRequestDto.TicketRequest t : dto.getTickets()) {
                Ticket ticket = new Ticket();
                ticket.setEvent(event);
                ticket.setTicketType(t.getTicketType());
                ticket.setQuantityTotal(t.getQuantityTotal());
                ticket.setQuantitySold(0);
                ticket.setPrice(t.getPrice());
                ticket.setSaleStart(dto.getSaleStart());
                ticket.setSaleEnd(dto.getSaleEnd());
                ticketRepository.save(ticket);
            }

            if (dto.getDiscounts() != null) {
                for (EventRequestDto.DiscountRequest d : dto.getDiscounts()) {
                    Discount discount = new Discount();
                    discount.setEvent(event);
                    discount.setCode(d.getDiscountCode());
                    discount.setDescription(d.getDiscountDescription());
                    discount.setDiscountType(DiscountType.valueOf(d.getDiscountType()));
                    discount.setValue(d.getDiscountValue());
                    discount.setValidFrom(d.getDiscountValidFrom());
                    discount.setValidTo(d.getDiscountValidTo());
                    discount.setMaxUsage(d.getDiscountMaxUses());
                    discountRepository.save(discount);
                }
            }
            return event.getEventId();
        });
    }

    public void updateEvent(Integer eventId,
                            UpdateEventRequestDto dto,
                            MultipartFile logoFile,
                            MultipartFile backgroundFile) {
        Integer organizerId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.hasRole(UserRole.organizer)) {
            throw new SecurityException("You do not have permission to update events.");
        }
        if (dto.getStartTime() != null && dto.getEndTime() != null && dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        if (logoFile != null && !logoFile.isEmpty()) {
            imageStorageService.validateImageFile(logoFile);
        }
        if (backgroundFile != null && !backgroundFile.isEmpty()) {
            imageStorageService.validateImageFile(backgroundFile);
        }

        String uploadedLogoUrl = (logoFile != null && !logoFile.isEmpty())
                ? imageStorageService.uploadImage(logoFile) : null;
        String uploadedBackgroundUrl = (backgroundFile != null && !backgroundFile.isEmpty())
                ? imageStorageService.uploadImage(backgroundFile) : null;

        transactionTemplate.executeWithoutResult(status -> {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

            if (!event.getOrganizer().getId().equals(organizerId)) {
                throw new SecurityException("You do not have permission to update this event.");
            }

            if (uploadedLogoUrl != null) {
                event.setLogoUrl(uploadedLogoUrl);
            }
            if (uploadedBackgroundUrl != null) {
                event.setBackgroundUrl(uploadedBackgroundUrl);
            }

            event.setEventName(dto.getEventName());
            event.setDescription(dto.getDescription());
            event.setAddressName(dto.getAddressName());
            event.setAddressDetail(dto.getAddressDetail());
            if (dto.getWardId() != null) {
                Ward ward = wardRepository.findById(dto.getWardId())
                        .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + dto.getWardId()));
                event.setWard(ward);
            }
            if (dto.getStartTime() != null) {
                event.setStartTime(dto.getStartTime());
            }
            if (dto.getEndTime() != null) {
                event.setEndTime(dto.getEndTime());
            }
            event.setCategory(dto.getCategory());
            event.setUpdatedAt(LocalDateTime.now());
            event.setApprovalStatus(ApprovalStatus.pending);
            eventRepository.save(event);

            syncTickets(event, dto.getTicketTypes());
            syncDiscounts(event, dto.getDiscounts());
        });
    }

    private void syncTickets(Event event, List<UpdateEventRequestDto.TicketTypeDto> ticketDtos) {
        if (ticketDtos == null) {
            return;
        }
        Set<String> incomingTicketTypes = ticketDtos.stream()
                .map(UpdateEventRequestDto.TicketTypeDto::getTicketType)
                .collect(Collectors.toSet());

        List<Ticket> existingTickets = ticketRepository.findByEvent(event);

        // Only block deletion of removed ticket types that already have orders
        for (Ticket existing : existingTickets) {
            if (!incomingTicketTypes.contains(existing.getTicketType())) {
                if (orderTicketRepository.existsByTicket(existing)) {
                    throw new IllegalArgumentException(
                            "Không thể xóa loại vé đã có đơn hàng: " + existing.getTicketType());
                }
                ticketRepository.delete(existing);
            }
        }

        // Update existing or create new ticket types
        for (UpdateEventRequestDto.TicketTypeDto dto : ticketDtos) {
            Optional<Ticket> existingOpt = existingTickets.stream()
                    .filter(t -> t.getTicketType().equals(dto.getTicketType()))
                    .findFirst();

            if (existingOpt.isPresent()) {
                Ticket ticket = existingOpt.get();
                if (dto.getQuantityTotal() != null && dto.getQuantityTotal() < ticket.getQuantitySold()) {
                    throw new IllegalArgumentException(
                            "Tổng số lượng vé không thể nhỏ hơn số vé đã bán (" + ticket.getQuantitySold() + ")");
                }
                ticket.setQuantityTotal(dto.getQuantityTotal());
                ticket.setPrice(dto.getPrice());
                ticketRepository.save(ticket);
            } else {
                Ticket newTicket = new Ticket();
                newTicket.setEvent(event);
                newTicket.setTicketType(dto.getTicketType());
                newTicket.setQuantityTotal(dto.getQuantityTotal());
                newTicket.setPrice(dto.getPrice());
                newTicket.setQuantitySold(0);
                ticketRepository.save(newTicket);
            }
        }
    }

    private void syncDiscounts(Event event, List<UpdateEventRequestDto.DiscountDto> discountDtos) {
        if (discountDtos == null) {
            return;
        }
        Set<String> incomingCodes = discountDtos.stream()
                .map(UpdateEventRequestDto.DiscountDto::getCode)
                .collect(Collectors.toSet());

        List<Discount> existingDiscounts = discountRepository.findByEvent(event);
        for (Discount existing : existingDiscounts) {
            if (!incomingCodes.contains(existing.getCode())) {
                discountRepository.delete(existing);
            }
        }

        for (UpdateEventRequestDto.DiscountDto dto : discountDtos) {
            Optional<Discount> existingOpt = existingDiscounts.stream()
                    .filter(d -> d.getCode().equals(dto.getCode()))
                    .findFirst();

            Discount target = existingOpt.orElseGet(() -> {
                Discount d = new Discount();
                d.setEvent(event);
                d.setCode(dto.getCode());
                return d;
            });
            target.setDescription(dto.getDescription());
            target.setDiscountType(DiscountType.valueOf(dto.getType()));
            target.setValue(dto.getValue());
            target.setValidFrom(dto.getValidFrom());
            target.setValidTo(dto.getValidTo());
            target.setMaxUsage(dto.getMaxUses());
            discountRepository.save(target);
        }
    }
}
