package com.ips.controller;

import com.ips.model.ParkingSlot;
import com.ips.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    @Autowired private SlotService slotService;

    @GetMapping("/facility/{facilityId}")
    public List<ParkingSlot> getSlots(@PathVariable String facilityId) {
        return slotService.getSlotsByFacility(facilityId);
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleSlot(@PathVariable String id) {
        try {
            ParkingSlot slot = slotService.toggleSlotOccupancy(id);
            return ResponseEntity.ok(slot);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
