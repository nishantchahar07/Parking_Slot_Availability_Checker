package com.ips.repository;

import com.ips.model.ParkingSlot;
import com.ips.model.enums.SlotType;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ParkingSlotRepository extends MongoRepository<ParkingSlot, String> {
    List<ParkingSlot> findByFacilityId(String facilityId);
    List<ParkingSlot> findByFacilityIdAndSlotType(String facilityId, SlotType slotType);
    long countByFacilityIdAndIsOccupiedFalseAndIsReservedFalse(String facilityId);
    ParkingSlot findTopByFacilityIdAndSlotTypeAndIsOccupiedFalseAndIsReservedFalse(String facilityId, SlotType slotType);
    void deleteByFacilityId(String facilityId);
}