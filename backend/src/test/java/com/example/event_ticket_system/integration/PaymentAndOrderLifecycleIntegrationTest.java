package com.example.event_ticket_system.integration;

import com.example.event_ticket_system.dto.request.OrderRequestDto;
import com.example.event_ticket_system.entity.*;
import com.example.event_ticket_system.enums.*;
import com.example.event_ticket_system.repository.*;
import com.example.event_ticket_system.security.CustomUserPrincipal;
import com.example.event_ticket_system.service.EmailService;
import com.example.event_ticket_system.service.OrderService;
import com.example.event_ticket_system.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentAndOrderLifecycleIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;
    @Autowired private ObjectMapper objectMapper;
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
    @MockitoBean private EmailService emailService;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private User admin;
    private User customer;
    private Event event;
    private Ticket ticket;
    private Discount discount;

    @BeforeEach
    void setUp() throws Exception {
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

        admin = saveUser("Admin", "admin@lifecycle.test", UserRole.admin);
        User organizer = saveUser("Org", "org@lifecycle.test", UserRole.organizer);
        customer = saveUser("Customer", "customer@lifecycle.test", UserRole.customer);

        Province province = provinceRepository.save(new Province(null, "Đà Nẵng"));
        District district = districtRepository.save(new District(null, province, "Hải Châu"));
        Ward ward = wardRepository.save(new Ward(null, district, "Thạch Thang"));

        Event e = new Event();
        e.setOrganizer(organizer);
        e.setEventName("Tech Expo 2026");
        e.setDescription("Annual conference");
        e.setWard(ward);
        e.setStartTime(LocalDateTime.now().plusDays(10));
        e.setEndTime(LocalDateTime.now().plusDays(11));
        e.setCategory("Workshop");
        e.setStatus(EventStatus.upcoming);
        e.setApprovalStatus(ApprovalStatus.approved);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        event = eventRepository.save(e);

        Ticket t = new Ticket();
        t.setEvent(event);
        t.setTicketType("Standard");
        t.setPrice(new BigDecimal("200000.00"));
        t.setQuantityTotal(10);
        t.setQuantitySold(0);
        t.setSaleStart(LocalDateTime.now().minusDays(1));
        t.setSaleEnd(LocalDateTime.now().plusDays(9));
        ticket = ticketRepository.save(t);

        Discount d = new Discount();
        d.setCode("EXPO50K");
        d.setDescription("50k off");
        d.setDiscountType(DiscountType.fixed_amount);
        d.setValue(new BigDecimal("50000.00"));
        d.setValidFrom(LocalDate.now().minusDays(1));
        d.setValidTo(LocalDate.now().plusDays(10));
        d.setEvent(event);
        d.setMaxUsage(2);
        discount = discountRepository.save(d);

        when(payOS.createPaymentLink(any(PaymentData.class))).thenAnswer(inv -> {
            PaymentData data = inv.getArgument(0);
            return CheckoutResponseData.builder()
                    .bin("970422")
                    .accountNumber("0123456789")
                    .accountName("EVENTA TEST")
                    .amount(data.getAmount())
                    .description(data.getDescription())
                    .orderCode(data.getOrderCode())
                    .currency("VND")
                    .paymentLinkId("pl-" + data.getOrderCode())
                    .status("PENDING")
                    .checkoutUrl("https://payos.test/checkout/" + data.getOrderCode())
                    .qrCode("mock-qr-data")
                    .build();
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User saveUser(String name, String email, UserRole role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash("hashed");
        u.setRole(role);
        u.setStatus(UserStatus.active);
        return userRepository.save(u);
    }

    private void authenticateAs(User user) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void payosWebhook_DeliveredTwice_ShouldBeIdempotentAndSendEmailAndCreatePaymentOnce() throws Exception {
        authenticateAs(customer);
        OrderRequestDto dto = new OrderRequestDto(
                event.getEventId(),
                List.of(new OrderRequestDto.OrderTicketRequestDto(ticket.getTicketId(), 2)),
                "EXPO50K",
                "http://localhost/success",
                "http://localhost/cancel"
        );
        CheckoutResponseData checkout = orderService.createOrder(dto);
        long payosCode = checkout.getOrderCode();

        // 2 * 200,000 - 50,000 = 350,000
        assertThat(checkout.getAmount()).isEqualTo(350_000);

        WebhookData verifiedData = WebhookData.builder()
                .orderCode(payosCode)
                .amount(350_000)
                .description("Thanh toan Eventa")
                .accountNumber("0123456789")
                .reference("TXN-REF-12345")
                .transactionDateTime("2026-01-01 10:00:00")
                .currency("VND")
                .paymentLinkId("pl-" + payosCode)
                .code("00")
                .desc("success")
                .build();
        when(payOS.verifyPaymentWebhookData(any(Webhook.class))).thenReturn(verifiedData);

        ObjectNode webhookPayload = objectMapper.createObjectNode();
        webhookPayload.put("code", "00");
        webhookPayload.put("desc", "success");
        webhookPayload.put("success", true);
        webhookPayload.set("data", objectMapper.valueToTree(verifiedData));
        webhookPayload.put("signature", "valid-sig");

        // First webhook delivery -> transitions pending -> paid
        ObjectNode firstRes = paymentService.handlePayOsWebhook(webhookPayload);
        assertThat(firstRes.get("error").asInt()).isEqualTo(0);
        assertThat(firstRes.get("message").asText()).isEqualTo("Webhook delivered");

        // Duplicate second webhook delivery -> idempotent no-op
        ObjectNode secondRes = paymentService.handlePayOsWebhook(webhookPayload);
        assertThat(secondRes.get("error").asInt()).isEqualTo(0);
        assertThat(secondRes.get("message").asText()).isEqualTo("Webhook already processed");

        // Wait for AFTER_COMMIT async email listener and verify strictly 1 invocation
        verify(emailService, timeout(3000).times(1))
                .sendOrderConfirmationEmail(eq(customer.getEmail()), anyString(), anyString(), anyList());

        Order paidOrder = orderRepository.findByPayosOrderCode(payosCode).orElseThrow();
        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.paid);
        assertThat(paymentRepository.count()).isEqualTo(1);

        List<OrderTicket> orderTickets = orderTicketRepository.findByOrder(paidOrder);
        assertThat(orderTickets).hasSize(1);
        OrderTicket ot = orderTickets.getFirst();
        assertThat(ot.getCheckinToken()).isNotBlank();
        assertThat(paymentService.verifyCheckinToken(ot.getCheckinToken())).isTrue();
        assertThat(paymentService.verifyCheckinToken(ot.getCheckinToken() + "tampered")).isFalse();

        // Attempting to cancel an already-paid order must fail with IllegalStateException
        authenticateAs(admin);
        assertThatThrownBy(() -> orderService.cancelOrder(payosCode, "Attempt cancel paid"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending orders can be cancelled");
    }

    @Test
    void cancelOrder_CalledTwice_ShouldRejectSecondCancelAndNotOverRestoreInventory() throws Exception {
        authenticateAs(customer);
        OrderRequestDto dto = new OrderRequestDto(
                event.getEventId(),
                List.of(new OrderRequestDto.OrderTicketRequestDto(ticket.getTicketId(), 2)),
                "EXPO50K",
                "http://localhost/success",
                "http://localhost/cancel"
        );
        CheckoutResponseData checkout = orderService.createOrder(dto);
        long payosCode = checkout.getOrderCode();

        // Inventory reserved: quantitySold = 2, discount.maxUsage = 1
        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getQuantitySold()).isEqualTo(2);
        assertThat(discountRepository.findById(discount.getDiscountId()).orElseThrow().getMaxUsage()).isEqualTo(1);

        when(payOS.cancelPaymentLink(eq(payosCode), anyString()))
                .thenReturn(PaymentLinkData.builder()
                        .id("pl-" + payosCode)
                        .orderCode(payosCode)
                        .amount(350_000)
                        .amountPaid(0)
                        .amountRemaining(350_000)
                        .status("CANCELLED")
                        .createdAt("2026-01-01T10:00:00Z")
                        .transactions(List.of())
                        .build());

        // First cancel by admin -> succeeds and restores inventory
        authenticateAs(admin);
        PaymentLinkData cancelResult = orderService.cancelOrder(payosCode, "Customer requested cancellation");
        assertThat(cancelResult).isNotNull();
        assertThat(orderRepository.findByPayosOrderCode(payosCode).orElseThrow().getStatus()).isEqualTo(OrderStatus.cancelled);
        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getQuantitySold()).isEqualTo(0);
        assertThat(discountRepository.findById(discount.getDiscountId()).orElseThrow().getMaxUsage()).isEqualTo(2);

        // Second duplicate cancel -> rejected; inventory must NOT drift
        assertThatThrownBy(() -> orderService.cancelOrder(payosCode, "Duplicate cancel"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending orders can be cancelled");

        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getQuantitySold()).isEqualTo(0);
        assertThat(discountRepository.findById(discount.getDiscountId()).orElseThrow().getMaxUsage()).isEqualTo(2);
    }
}
