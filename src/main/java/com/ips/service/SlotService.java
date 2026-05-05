package com.ips.service;

import com.ips.model.ParkingSlot;
import com.ips.repository.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SlotService {

    @Autowired private ParkingSlotRepository slotRepo;
    @Autowired(required = false) private SimpMessagingTemplate messagingTemplate;

    public List<ParkingSlot> getSlotsByFacility(String facilityId) {
        return slotRepo.findByFacilityId(facilityId);
    }

    public ParkingSlot toggleSlotOccupancy(String slotId) {
        ParkingSlot slot = slotRepo.findById(slotId).orElseThrow(() -> new RuntimeException("Slot not found"));
        slot.setOccupied(!slot.isOccupied());
        if (!slot.isOccupied()) {
            slot.setVehiclePlate(null);
            slot.setReserved(false);
            slot.setReservedBy(null);
        }
        slot = slotRepo.save(slot);
        broadcastUpdate(slot.getFacilityId());
        return slot;
    }

    public void broadcastUpdate(String facilityId) {
        if (messagingTemplate != null) {
            List<ParkingSlot> slots = slotRepo.findByFacilityId(facilityId);
            messagingTemplate.convertAndSend("/topic/slots/" + facilityId, slots);
        }
    }
}
