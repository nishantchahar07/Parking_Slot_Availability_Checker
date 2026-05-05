package com.ips.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BookingRequest {
    private String slotId;
    private String facilityId;
    private String vehiclePlate;
    private String vehicleType;
}
