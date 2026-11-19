package com.example.event_ticket_system.integration;

import com.example.event_ticket_system.entity.*;
import com.example.event_ticket_system.enums.*;
import com.example.event_ticket_system.repository.*;
import com.example.event_ticket_system.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndRbacIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private DistrictRepository districtRepository;
    @Autowired private WardRepository wardRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private DiscountRepository discountRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderTicketRepository orderTicketRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ReviewRepository reviewRepository;

    @MockitoBean private PayOS payOS;
    @MockitoBean private JavaMailSender mailSender;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private String adminToken;
    private String organizerAToken;
    private String organizerBToken;
    private String customerToken;
    private Event eventA;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderTicketRepository.deleteAll();
        orderRepository.deleteAll();
        reviewRepository.deleteAll();
        discountRepository.deleteAll();
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        wardRepository.deleteAll();
        districtRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        User admin = saveUser("Admin User", "admin@test.com", UserRole.admin);
        User organizerA = saveUser("Organizer A", "orga@test.com", UserRole.organizer);
        User organizerB = saveUser("Organizer B", "orgb@test.com", UserRole.organizer);
        User customer = saveUser("Customer User", "cust@test.com", UserRole.customer);

        adminToken = jwtUtil.generateToken(admin.getFullName(), admin.getEmail(), admin.getRole().name(), admin.getId());
        organizerAToken = jwtUtil.generateToken(organizerA.getFullName(), organizerA.getEmail(), organizerA.getRole().name(), organizerA.getId());
        organizerBToken = jwtUtil.generateToken(organizerB.getFullName(), organizerB.getEmail(), organizerB.getRole().name(), organizerB.getId());
        customerToken = jwtUtil.generateToken(customer.getFullName(), customer.getEmail(), customer.getRole().name(), customer.getId());

        Province province = provinceRepository.save(new Province(null, "Hà Nội"));
        District district = districtRepository.save(new District(null, province, "Ba Đình"));
        Ward ward = wardRepository.save(new Ward(null, district, "Kim Mã"));

        Event e = new Event();
        e.setOrganizer(organizerA);
        e.setEventName("Grand Concert 2026");
        e.setDescription("Live orchestra concert");
        e.setWard(ward);
        e.setAddressName("Opera House");
        e.setAddressDetail("1 Tràng Tiền");
        e.setStartTime(LocalDateTime.now().plusDays(10));
        e.setEndTime(LocalDateTime.now().plusDays(10).plusHours(3));
        e.setCategory("Live Music");
        e.setStatus(EventStatus.upcoming);
        e.setApprovalStatus(ApprovalStatus.approved);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        eventA = eventRepository.save(e);

        Ticket vip = new Ticket();
        vip.setEvent(eventA);
        vip.setTicketType("VIP");
        vip.setPrice(new BigDecimal("250000.00"));
        vip.setQuantityTotal(50);
        vip.setQuantitySold(2);
        vip.setSaleStart(LocalDateTime.now().minusDays(1));
        vip.setSaleEnd(LocalDateTime.now().plusDays(9));
        vip = ticketRepository.save(vip);

        Discount discount = new Discount();
        discount.setCode("SAVE20");
        discount.setDescription("20% off concert tickets");
        discount.setDiscountType(DiscountType.percentage);
        discount.setValue(new BigDecimal("20.00"));
        discount.setValidFrom(LocalDate.now().minusDays(1));
        discount.setValidTo(LocalDate.now().plusDays(10));
        discount.setEvent(eventA);
        discount.setMaxUsage(10);
        discountRepository.save(discount);

        Order paidOrder = new Order();
        paidOrder.setUser(customer);
        paidOrder.setOrderDate(LocalDateTime.now());
        paidOrder.setTotalAmount(new BigDecimal("500000.00"));
        paidOrder.setStatus(OrderStatus.paid);
        paidOrder.setPayosOrderCode(900001001L);
        paidOrder = orderRepository.save(paidOrder);

        OrderTicket ot = new OrderTicket();
        ot.setOrder(paidOrder);
        ot.setTicket(vip);
        ot.setQuantity(2);
        ot.setUnitPrice(new BigDecimal("250000.00"));
        orderTicketRepository.save(ot);
    }

    private User saveUser(String fullName, String email, UserRole role) {
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPasswordHash("$2a$10$dummyhashedpasswordvaluefortestingonly123456789012345");
        u.setRole(role);
        u.setStatus(UserStatus.active);
        return userRepository.save(u);
    }

    @Test
    void adminEndpoints_ShouldEnforceRbac() throws Exception {
        // Unauthenticated -> 401
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/users/delete").param("userIds", "1"))
                .andExpect(status().isUnauthorized());

        // Customer & Organizer -> 403
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + organizerAToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/users/delete").param("userIds", "1")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/users/disable").param("userIds", "1")
                        .header("Authorization", "Bearer " + organizerAToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/events/pending").header("Authorization", "Bearer " + organizerAToken))
                .andExpect(status().isForbidden());

        // Admin -> 200
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(get("/api/events/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void reportEndpoints_ShouldBlockCrossOrganizerIdorAndRenderValidPdfAndExcelForOwnerAndAdmin() throws Exception {
        // Unauthenticated -> 401
        mockMvc.perform(get("/api/events/{id}/report/buyer-pdf", eventA.getEventId()))
                .andExpect(status().isUnauthorized());

        // Organizer B (non-owner) -> 403 IDOR blocked
        mockMvc.perform(get("/api/events/{id}/report/buyer-excel", eventA.getEventId())
                        .header("Authorization", "Bearer " + organizerBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/events/{id}/report/buyer-pdf", eventA.getEventId())
                        .header("Authorization", "Bearer " + organizerBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/events/{id}/report/pdf", eventA.getEventId())
                        .header("Authorization", "Bearer " + organizerBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/events/report/ticket-per-day/{id}", eventA.getEventId())
                        .header("Authorization", "Bearer " + organizerBToken))
                .andExpect(status().isForbidden());

        // Customer -> 403
        mockMvc.perform(get("/api/events/{id}/report/buyer-pdf", eventA.getEventId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Organizer A (owner) -> 200 with valid PDF bytes (verifies classpath fonts/arial.ttf works!)
        byte[] ownerPdf = mockMvc.perform(get("/api/events/{id}/report/pdf", eventA.getEventId())
                        .header("Authorization", "Bearer " + organizerAToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(ownerPdf, 0, 5)).isEqualTo("%PDF-");

        byte[] buyerPdf = mockMvc.perform(get("/api/events/{id}/report/buyer-pdf", eventA.getEventId())
                        .header("Authorization", "Bearer " + organizerAToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(buyerPdf, 0, 5)).isEqualTo("%PDF-");

        // Admin -> 200 with valid XLSX bytes (PK zip header)
        byte[] buyerExcel = mockMvc.perform(get("/api/events/{id}/report/buyer-excel", eventA.getEventId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(buyerExcel.length).isGreaterThan(100);
        assertThat(buyerExcel[0]).isEqualTo((byte) 'P');
        assertThat(buyerExcel[1]).isEqualTo((byte) 'K');
    }

    @Test
    void getDiscountByCode_ShouldNotLeakOrganizerOrPasswordHash() throws Exception {
        mockMvc.perform(get("/api/discounts/{code}", "SAVE20")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SAVE20"))
                .andExpect(jsonPath("$.data.eventId").value(eventA.getEventId()))
                .andExpect(jsonPath("$.data.eventName").value("Grand Concert 2026"))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("dummyhashedpassword"))))
                .andExpect(content().string(not(containsString("organizer"))));
    }

    @Test
    void organizerSearchByName_ShouldNotCrashAndShouldMatchEventName() throws Exception {
        // Regression test for legacy bug: root.get("") threw IllegalArgumentException
        mockMvc.perform(get("/api/events/by-organizer")
                        .param("name", "Concert")
                        .header("Authorization", "Bearer " + organizerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listEvents.length()").value(1))
                .andExpect(jsonPath("$.data.listEvents[0].eventName").value("Grand Concert 2026"));

        // Non-matching query returns empty list without crashing
        mockMvc.perform(get("/api/events/by-organizer")
                        .param("name", "NoSuchEvent")
                        .header("Authorization", "Bearer " + organizerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listEvents.length()").value(0));
    }

    @Test
    void publicCatalogEndpoints_ShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/events/recommend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listEvents.length()").value(1));

        mockMvc.perform(get("/api/events/{eventId}", eventA.getEventId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventName").value("Grand Concert 2026"));
    }
}
