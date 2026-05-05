package com.ips.dto;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class FacilityRequest {
    private String name;
    private String address;
    private String city;
    private String state;
    private double latitude;
    private double longitude;
    private int totalSlots;
    private double pricePerHour;
    private String facilityType;
    private List<String> amenities;
    private String operatingHours;
    // Slot distribution
    private int standardSlots;
    private int coveredSlots;
    private int evChargingSlots;
    private int handicapSlots;
    private int openAirSlots;
}
