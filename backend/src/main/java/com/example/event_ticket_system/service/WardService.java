package com.example.event_ticket_system.service;

import com.example.event_ticket_system.entity.Ward;

import java.util.List;

public interface WardService {
    List<Ward> getWardsByDistrictId(Integer districtId);
}
