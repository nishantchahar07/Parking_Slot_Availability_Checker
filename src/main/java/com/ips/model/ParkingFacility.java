package com.ips.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "parking_facilities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingFacility {
    @Id
    private String id;
    private String name;
    private String address;
    private String city;
    private String state;
    private double latitude;
    private double longitude;
    private int totalSlots;
    private int availableSlots;
    private double pricePerHour;
    private String facilityType;
    private List<String> amenities;
    private String operatingHours;
    private String managerId;
    private String managerName;
    private boolean isActive;
    private LocalDateTime createdAt;
}
