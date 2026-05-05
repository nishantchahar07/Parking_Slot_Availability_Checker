package com.ips.controller;

import com.ips.dto.BookingRequest;
import com.ips.model.Booking;
import com.ips.security.JwtUtil;
import com.ips.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired private BookingService bookingService;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest req,
                                            @RequestHeader("Authorization") String auth) {
        try {
            String driverId = jwtUtil.extractUserId(auth.substring(7));
            Booking booking = bookingService.createBooking(req, driverId);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public List<Booking> getMyBookings(@RequestHeader("Authorization") String auth) {
        String driverId = jwtUtil.extractUserId(auth.substring(7));
        return bookingService.getDriverBookings(driverId);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeBooking(@PathVariable String id,
                                              @RequestHeader("Authorization") String auth) {
        try {
            String driverId = jwtUtil.extractUserId(auth.substring(7));
            Map<String, Object> result = bookingService.completeBooking(id, driverId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String id,
                                            @RequestHeader("Authorization") String auth) {
        try {
            String driverId = jwtUtil.extractUserId(auth.substring(7));
            Booking booking = bookingService.cancelBooking(id, driverId);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/facility/{facilityId}")
    public List<Booking> getFacilityBookings(@PathVariable String facilityId) {
        return bookingService.getFacilityBookings(facilityId);
    }
}
