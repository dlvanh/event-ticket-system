package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

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

}
