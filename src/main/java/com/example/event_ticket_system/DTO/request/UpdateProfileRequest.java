package com.example.event_ticket_system.DTO.request;

import com.example.event_ticket_system.Enums.Gender;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    public Integer id;
    public String fullName;
    public String email;
    public String phoneNumber;
    public Enum<Gender> gender;
    public String address;
    public String bio;

}
