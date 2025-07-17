package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.OrderRequestDto;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Ticket;
import com.example.event_ticket_system.Repository.EventRepository;
import com.example.event_ticket_system.Repository.TicketRepository;
import com.example.event_ticket_system.Service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import vn.payos.type.CheckoutResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class CheckoutController {
    private final OrderService orderService;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;

    public CheckoutController(OrderService orderService, EventRepository eventRepository, TicketRepository ticketRepository) {
        this.orderService = orderService;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    @RequestMapping(value = "/success")
    public String success() {
        return "success";
    }

    @RequestMapping(value = "/cancel")
    public String cancel() {
        return "cancel";
    }

    @RequestMapping(value = "/")
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/event/{eventId}")
    public String index(@RequestParam("eventId") Integer eventId, Model model) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid event ID"));
        List<Ticket> tickets = ticketRepository.findByEventEventId(eventId);
        model.addAttribute("event", event);
        model.addAttribute("tickets", tickets);
        return "index";
    }

    @RequestMapping(method = RequestMethod.POST, value = "/create-payment-link", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void checkout(HttpServletRequest request, HttpServletResponse httpServletResponse) {
        try {
            Integer eventId = Integer.parseInt(request.getParameter("eventId"));
            String discountCode = request.getParameter("discountCode");
            String returnUrl = getBaseUrl(request) + "/success";
            String cancelUrl = getBaseUrl(request) + "/cancel";

            // Process ticket selections
            Map<String, String[]> parameterMap = request.getParameterMap();
            List<OrderRequestDto.OrderTicketRequestDto> ticketDtos = new ArrayList<>();
            for (String key : parameterMap.keySet()) {
                if (key.startsWith("tickets[")) {
                    String ticketIdStr = key.substring(8, key.indexOf("].quantity"));
                    Integer ticketId = Integer.parseInt(ticketIdStr);
                    int quantity = Integer.parseInt(request.getParameter(key));
                    if (quantity > 0) {
                        OrderRequestDto.OrderTicketRequestDto ticketDto = new OrderRequestDto.OrderTicketRequestDto();
                        ticketDto.setTicketId(ticketId);
                        ticketDto.setQuantity(quantity);
                        ticketDtos.add(ticketDto);
                    }
                }
            }

            if (ticketDtos.isEmpty()) {
                throw new IllegalArgumentException("No tickets selected");
            }

            // Create OrderRequestDto
            OrderRequestDto orderRequestDto = new OrderRequestDto();
            orderRequestDto.setEventId(eventId);
            orderRequestDto.setTickets(ticketDtos);
            orderRequestDto.setDiscountCode(discountCode);
            orderRequestDto.setReturnUrl(returnUrl);
            orderRequestDto.setCancelUrl(cancelUrl);

            // Call OrderService to create order and payment link
            CheckoutResponseData data = orderService.createOrder(orderRequestDto, request);

            // Redirect to PayOS checkout URL
            httpServletResponse.setHeader("Location", data.getCheckoutUrl());
            httpServletResponse.setStatus(302);
        } catch (Exception e) {
            e.printStackTrace();
            httpServletResponse.setStatus(500);
            // Optionally redirect to an error page
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        log.info("Scheme: {}, Server Name: {}, Server Port: {}, Context Path: {}", scheme, serverName, serverPort, contextPath);

        String url = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("http") && serverPort != 81) || (scheme.equals("https") && serverPort != 443)) {
            url += ":" + serverPort;
        }
        url += contextPath;
        return url;
    }
}