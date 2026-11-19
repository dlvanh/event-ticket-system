package com.example.event_ticket_system.DTO.request;

public class VerifyCodeDTO {
    private String email;
    private String code;

    // getters, setters
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
}
