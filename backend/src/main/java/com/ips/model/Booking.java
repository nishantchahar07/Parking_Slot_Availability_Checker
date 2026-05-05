package com.ips.model;

import com.ips.model.enums.BookingStatus;
import com.ips.model.enums.SlotType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    private String id;
    private String driverId;
    private String driverName;
    private String driverEmail;
    private String slotId;
    private String facilityId;
    private String facilityName;
    private String slotNumber;
    private SlotType slotType;
    private String vehiclePlate;
    private String vehicleType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private double totalFee;
    private double pricePerHour;
    private String paymentStatus;
    private LocalDateTime createdAt;
}
