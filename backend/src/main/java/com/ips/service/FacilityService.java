package com.ips.service;

import com.ips.dto.FacilityRequest;
import com.ips.model.ParkingFacility;
import com.ips.model.ParkingSlot;
import com.ips.model.enums.SlotType;
import com.ips.repository.ParkingFacilityRepository;
import com.ips.repository.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FacilityService {

    @Autowired private ParkingFacilityRepository facilityRepo;
    @Autowired private ParkingSlotRepository slotRepo;

    public ParkingFacility createFacility(FacilityRequest req, String managerId, String managerName) {
        int total = req.getStandardSlots() + req.getCoveredSlots() + req.getEvChargingSlots()
                + req.getHandicapSlots() + req.getOpenAirSlots();
        if (total == 0) total = req.getTotalSlots() > 0 ? req.getTotalSlots() : 20;

        ParkingFacility facility = ParkingFacility.builder()
                .name(req.getName()).address(req.getAddress()).city(req.getCity()).state(req.getState())
                .latitude(req.getLatitude()).longitude(req.getLongitude())
                .totalSlots(total).availableSlots(total).pricePerHour(req.getPricePerHour())
                .facilityType(req.getFacilityType()).amenities(req.getAmenities())
                .operatingHours(req.getOperatingHours())
                .managerId(managerId).managerName(managerName)
                .isActive(true).createdAt(LocalDateTime.now())
                .build();
        facility = facilityRepo.save(facility);
        generateSlots(facility.getId(), req);
        return facility;
    }

    private void generateSlots(String facilityId, FacilityRequest req) {
        List<ParkingSlot> slots = new ArrayList<>();
        int counter = 1;
        counter = addSlots(slots, facilityId, SlotType.HANDICAP, req.getHandicapSlots(), counter, "G");
        counter = addSlots(slots, facilityId, SlotType.EV_CHARGING, req.getEvChargingSlots(), counter, "G");
        counter = addSlots(slots, facilityId, SlotType.COVERED, req.getCoveredSlots(), counter, "L1");
        counter = addSlots(slots, facilityId, SlotType.OPEN_AIR, req.getOpenAirSlots(), counter, "G");
        int stdCount = req.getStandardSlots();
        if (stdCount == 0 && req.getTotalSlots() > 0) {
            stdCount = req.getTotalSlots() - (req.getHandicapSlots() + req.getEvChargingSlots()
                    + req.getCoveredSlots() + req.getOpenAirSlots());
            if (stdCount <= 0) stdCount = req.getTotalSlots();
        }
        if (stdCount == 0) stdCount = 20;
        addSlots(slots, facilityId, SlotType.STANDARD, stdCount, counter, "G");
        slotRepo.saveAll(slots);
    }

    private int addSlots(List<ParkingSlot> slots, String facilityId, SlotType type, int count, int start, String floor) {
        for (int i = 0; i < count; i++) {
            String prefix = type.name().substring(0, 1);
            slots.add(ParkingSlot.builder()
                    .facilityId(facilityId)
                    .slotNumber(prefix + "-" + String.format("%02d", start + i))
                    .floor(floor).slotType(type)
                    .isOccupied(false).isReserved(false)
                    .build());
        }
        return start + count;
    }

    public List<ParkingFacility> searchFacilities(String city, String type) {
        List<ParkingFacility> facilities;
        if (city != null && !city.isBlank()) {
            facilities = facilityRepo.findByCityContainingIgnoreCaseAndIsActiveTrue(city);
        } else {
            facilities = facilityRepo.findByIsActiveTrue();
        }
        // Update available counts
        for (ParkingFacility f : facilities) {
            long avail = slotRepo.countByFacilityIdAndIsOccupiedFalseAndIsReservedFalse(f.getId());
            f.setAvailableSlots((int) avail);
        }
        if (type != null && !type.isBlank() && !type.equals("ALL")) {
            // Filter facilities that have slots of the requested type
            List<ParkingFacility> filtered = new ArrayList<>();
            for (ParkingFacility f : facilities) {
                List<ParkingSlot> typeSlots = slotRepo.findByFacilityIdAndSlotType(f.getId(), SlotType.valueOf(type));
                if (!typeSlots.isEmpty()) filtered.add(f);
            }
            return filtered;
        }
        return facilities;
    }

    public ParkingFacility getFacility(String id) {
        ParkingFacility f = facilityRepo.findById(id).orElseThrow(() -> new RuntimeException("Facility not found"));
        long avail = slotRepo.countByFacilityIdAndIsOccupiedFalseAndIsReservedFalse(f.getId());
        f.setAvailableSlots((int) avail);
        return f;
    }

    public List<ParkingFacility> getManagerFacilities(String managerId) {
        List<ParkingFacility> facilities = facilityRepo.findByManagerId(managerId);
        for (ParkingFacility f : facilities) {
            long avail = slotRepo.countByFacilityIdAndIsOccupiedFalseAndIsReservedFalse(f.getId());
            f.setAvailableSlots((int) avail);
        }
        return facilities;
    }

    public void deleteFacility(String id, String managerId) {
        ParkingFacility f = facilityRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (!f.getManagerId().equals(managerId)) throw new RuntimeException("Unauthorized");
        slotRepo.deleteByFacilityId(id);
        facilityRepo.deleteById(id);
    }
}
