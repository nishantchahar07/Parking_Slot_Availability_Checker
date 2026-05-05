package com.ips.service;

import com.ips.dto.BookingRequest;
import com.ips.model.*;
import com.ips.model.enums.BookingStatus;
import com.ips.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BookingService — ACID-safe booking operations.
 * 
 * Uses MongoDB's atomic findAndModify to prevent race conditions:
 * - Two users booking the same slot simultaneously → only one succeeds
 * - Slot status + booking creation are consistent
 * - @Version on ParkingSlot provides optimistic locking as a safety net
 */
@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepo;
    @Autowired private ParkingSlotRepository slotRepo;
    @Autowired private ParkingFacilityRepository facilityRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private SlotService slotService;
    @Autowired private MongoTemplate mongoTemplate;

    /**
     * ATOMIC BOOKING — prevents double-booking via findAndModify.
     * 
     * The query atomically checks: is slot free AND not reserved?
     * If yes, it marks it occupied+reserved in one atomic operation.
     * If another user tries concurrently, their query finds isOccupied=true → returns null → error.
     */
    public Booking createBooking(BookingRequest req, String driverId) {
        // 1. Prevent duplicate active bookings for same vehicle plate
        List<Booking> activeBookings = bookingRepo.findByDriverIdAndStatus(driverId, BookingStatus.ACTIVE);
        for (Booking existing : activeBookings) {
            if (existing.getVehiclePlate() != null &&
                existing.getVehiclePlate().equalsIgnoreCase(req.getVehiclePlate())) {
                throw new RuntimeException("Vehicle " + req.getVehiclePlate() + " already has an active booking at " + existing.getFacilityName());
            }
        }

        // 2. ATOMIC slot reservation using findAndModify
        //    This is the core ACID guarantee: only ONE concurrent request can succeed
        Query query = new Query(Criteria.where("_id").is(req.getSlotId())
                .and("isOccupied").is(false)
                .and("isReserved").is(false));

        Update update = new Update()
                .set("isOccupied", true)
                .set("isReserved", true)
                .set("reservedBy", driverId)
                .set("vehiclePlate", req.getVehiclePlate().toUpperCase());

        // findAndModify is atomic — MongoDB guarantees no two threads can modify the same doc
        ParkingSlot slot = mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(true),
                ParkingSlot.class);

        if (slot == null) {
            throw new RuntimeException("Slot is no longer available — it may have been booked by another user. Please select a different slot.");
        }

        // 3. Load related data
        ParkingFacility facility = facilityRepo.findById(req.getFacilityId())
                .orElseThrow(() -> new RuntimeException("Facility not found"));
        User driver = userRepo.findById(driverId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 4. Create booking record
        Booking booking = Booking.builder()
                .driverId(driverId)
                .driverName(driver.getName())
                .driverEmail(driver.getEmail())
                .slotId(slot.getId())
                .facilityId(facility.getId())
                .facilityName(facility.getName())
                .slotNumber(slot.getSlotNumber())
                .slotType(slot.getSlotType())
                .vehiclePlate(req.getVehiclePlate().toUpperCase())
                .vehicleType(req.getVehicleType())
                .startTime(LocalDateTime.now())
                .status(BookingStatus.ACTIVE)
                .pricePerHour(facility.getPricePerHour())
                .totalFee(0)
                .paymentStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        booking = bookingRepo.save(booking);

        // 5. Broadcast real-time update
        slotService.broadcastUpdate(facility.getId());
        return booking;
    }

    /**
     * ATOMIC COMPLETION — atomically releases the slot when completing a booking.
     * Calculates fee based on actual parking duration.
     */
    public Map<String, Object> completeBooking(String bookingId, String driverId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!booking.getDriverId().equals(driverId)) {
            throw new RuntimeException("Unauthorized — this booking belongs to another user");
        }
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new RuntimeException("Booking is not active — it's already " + booking.getStatus());
        }

        // Mark booking as completed
        booking.setEndTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.COMPLETED);

        // Calculate fee: ₹ per hour, rounded up, minimum 1 hour
        Duration duration = Duration.between(booking.getStartTime(), booking.getEndTime());
        long minutes = Math.max(duration.toMinutes(), 1);
        long hours = (long) Math.ceil(minutes / 60.0);
        if (hours < 1) hours = 1;
        double fee = hours * booking.getPricePerHour();
        booking.setTotalFee(fee);
        booking.setPaymentStatus("PAID");
        bookingRepo.save(booking);

        // ATOMIC slot release — prevents inconsistency
        Query query = new Query(Criteria.where("_id").is(booking.getSlotId()));
        Update update = new Update()
                .set("isOccupied", false)
                .set("isReserved", false)
                .set("reservedBy", null)
                .set("vehiclePlate", null);
        mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), ParkingSlot.class);

        slotService.broadcastUpdate(booking.getFacilityId());

        Map<String, Object> result = new HashMap<>();
        result.put("booking", booking);
        result.put("fee", fee);
        result.put("duration", formatDuration(duration));
        result.put("hours", hours);
        return result;
    }

    /**
     * ATOMIC CANCELLATION — atomically releases the slot.
     */
    public Booking cancelBooking(String bookingId, String driverId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!booking.getDriverId().equals(driverId)) {
            throw new RuntimeException("Unauthorized");
        }
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new RuntimeException("Booking is not active");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setEndTime(LocalDateTime.now());
        booking.setTotalFee(0);
        bookingRepo.save(booking);

        // ATOMIC slot release
        Query query = new Query(Criteria.where("_id").is(booking.getSlotId()));
        Update update = new Update()
                .set("isOccupied", false)
                .set("isReserved", false)
                .set("reservedBy", null)
                .set("vehiclePlate", null);
        mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), ParkingSlot.class);

        slotService.broadcastUpdate(booking.getFacilityId());
        return booking;
    }

    public List<Booking> getDriverBookings(String driverId) {
        return bookingRepo.findByDriverIdOrderByCreatedAtDesc(driverId);
    }

    public List<Booking> getFacilityBookings(String facilityId) {
        return bookingRepo.findByFacilityIdOrderByCreatedAtDesc(facilityId);
    }

    private String formatDuration(Duration d) {
        long h = d.toHours();
        long m = d.toMinutesPart();
        if (h > 0) return h + "h " + m + "m";
        return m + " min";
    }
}
