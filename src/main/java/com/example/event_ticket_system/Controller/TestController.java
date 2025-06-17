package com.example.event_ticket_system.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/admin/test")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<String> adminTest() {
        return ResponseEntity.ok("This is an ADMIN endpoint");
    }

    @GetMapping("/customer/test")
    @PreAuthorize("hasAuthority('ROLE_customer')")
    public ResponseEntity<String> customerTest() {
        return ResponseEntity.ok("This is a CUSTOMER endpoint");
    }

    @GetMapping("/organizer/test")
    @PreAuthorize("hasAuthority('ROLE_organizer')")
    public ResponseEntity<String> organizerTest() {
        return ResponseEntity.ok("This is an ORGANIZER endpoint");
    }

    @GetMapping("/public/test")
    public ResponseEntity<String> publicTest() {
        return ResponseEntity.ok("This is a PUBLIC endpoint");
    }
}