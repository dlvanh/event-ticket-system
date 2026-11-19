package com.example.event_ticket_system.integration;

import com.example.event_ticket_system.dto.request.OrderRequestDto;
import com.example.event_ticket_system.entity.*;
import com.example.event_ticket_system.enums.*;
import com.example.event_ticket_system.exception.ApiException;
import com.example.event_ticket_system.repository.*;
import com.example.event_ticket_system.security.CustomUserPrincipal;
import com.example.event_ticket_system.service.OrderService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyIntegrationTest {

    @Autowired private OrderService orderService;
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

    private User customer;
    private Event event;
    private Ticket limitedTicket;
    private Discount discount;

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

        User organizer = new User();
        organizer.setFullName("Org");
        organizer.setEmail("org@concurrency.test");
        organizer.setPasswordHash("hashed");
        organizer.setRole(UserRole.organizer);
        organizer.setStatus(UserStatus.active);
        organizer = userRepository.save(organizer);

        customer = new User();
        customer.setFullName("Buyer");
        customer.setEmail("buyer@concurrency.test");
        customer.setPasswordHash("hashed");
        customer.setRole(UserRole.customer);
        customer.setStatus(UserStatus.active);
        customer = userRepository.save(customer);

        Province province = provinceRepository.save(new Province(null, "TP.HCM"));
        District district = districtRepository.save(new District(null, province, "Quận 1"));
        Ward ward = wardRepository.save(new Ward(null, district, "Bến Nghé"));

        Event e = new Event();
        e.setOrganizer(organizer);
        e.setEventName("Flash Sale Concert");
        e.setDescription("Only 5 tickets available");
        e.setWard(ward);
        e.setStartTime(LocalDateTime.now().plusDays(5));
        e.setEndTime(LocalDateTime.now().plusDays(5).plusHours(2));
        e.setCategory("Live Music");
        e.setStatus(EventStatus.upcoming);
        e.setApprovalStatus(ApprovalStatus.approved);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        event = eventRepository.save(e);

        Ticket t = new Ticket();
        t.setEvent(event);
        t.setTicketType("EarlyBird");
        t.setPrice(new BigDecimal("100000.00"));
        t.setQuantityTotal(5);
        t.setQuantitySold(0);
        t.setSaleStart(LocalDateTime.now().minusHours(1));
        t.setSaleEnd(LocalDateTime.now().plusDays(4));
        limitedTicket = ticketRepository.save(t);

        Discount d = new Discount();
        d.setCode("FLASH10");
        d.setDescription("10% off");
        d.setDiscountType(DiscountType.percentage);
        d.setValue(new BigDecimal("10.00"));
        d.setValidFrom(LocalDate.now().minusDays(1));
        d.setValidTo(LocalDate.now().plusDays(5));
        d.setEvent(event);
        d.setMaxUsage(3);
        discount = discountRepository.save(d);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void concurrentOrders_ShouldNeverOversellBeyondQuantityTotal() throws Exception {
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

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                CustomUserPrincipal principal = new CustomUserPrincipal(
                        customer.getId(), customer.getEmail(), customer.getFullName(), UserRole.customer);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
                try {
                    readyLatch.countDown();
                    startLatch.await(10, TimeUnit.SECONDS);

                    OrderRequestDto dto = new OrderRequestDto(
                            event.getEventId(),
                            List.of(new OrderRequestDto.OrderTicketRequestDto(limitedTicket.getTicketId(), 1)),
                            null,
                            "http://localhost/success",
                            "http://localhost/cancel"
                    );
                    CheckoutResponseData res = orderService.createOrder(dto);
                    if (res != null && res.getOrderCode() != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }

        assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        Ticket reloadedTicket = ticketRepository.findById(limitedTicket.getTicketId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failureCount.get()).isEqualTo(15);
        assertThat(reloadedTicket.getQuantitySold()).isEqualTo(5);
        assertThat(orderRepository.count()).isEqualTo(5);
    }

    @Test
    void createOrder_WhenPayOsFails_ShouldCompensateAndRestoreTicketAndDiscountInventory() throws Exception {
        when(payOS.createPaymentLink(any(PaymentData.class)))
                .thenThrow(new RuntimeException("PayOS timeout"));

        CustomUserPrincipal principal = new CustomUserPrincipal(
                customer.getId(), customer.getEmail(), customer.getFullName(), UserRole.customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        OrderRequestDto dto = new OrderRequestDto(
                event.getEventId(),
                List.of(new OrderRequestDto.OrderTicketRequestDto(limitedTicket.getTicketId(), 2)),
                "FLASH10",
                "http://localhost/success",
                "http://localhost/cancel"
        );

        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Failed to create PayOS payment link");

        Ticket reloadedTicket = ticketRepository.findById(limitedTicket.getTicketId()).orElseThrow();
        Discount reloadedDiscount = discountRepository.findById(discount.getDiscountId()).orElseThrow();
        List<Order> orders = orderRepository.findAll();

        assertThat(reloadedTicket.getQuantitySold()).isEqualTo(0);
        assertThat(reloadedDiscount.getMaxUsage()).isEqualTo(3);
        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().getStatus()).isEqualTo(OrderStatus.cancelled);
    }
}
