package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.Entity.Ward;

import java.util.List;

public interface WardService {
    List<Ward> getWardsByDistrictId(Integer districtId);
}
