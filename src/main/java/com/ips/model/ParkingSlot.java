package com.ips.model;

import com.ips.model.enums.SlotType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "parking_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSlot {
    @Id
    private String id;
    private String facilityId;
    private String slotNumber;
    private String floor;
    private SlotType slotType;
    private boolean isOccupied;
    private boolean isReserved;
    private String reservedBy;
    private String vehiclePlate;
    private LocalDateTime reservationExpiry;

    /**
     * Optimistic locking version field.
     * MongoDB will reject concurrent writes to the same document
     * if the version has changed since it was read.
     */
    @Version
    private Long version;
}