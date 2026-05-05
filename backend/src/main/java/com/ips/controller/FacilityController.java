package com.ips.controller;

import com.ips.dto.FacilityRequest;
import com.ips.model.ParkingFacility;
import com.ips.security.JwtUtil;
import com.ips.service.FacilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    @Autowired private FacilityService facilityService;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping
    public List<ParkingFacility> searchFacilities(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type) {
        return facilityService.searchFacilities(city, type);
    }

    @GetMapping("/{id}")
    public ParkingFacility getFacility(@PathVariable String id) {
        return facilityService.getFacility(id);
    }

    @GetMapping("/my")
    public List<ParkingFacility> getMyFacilities(@RequestHeader("Authorization") String auth) {
        String token = auth.substring(7);
        String managerId = jwtUtil.extractUserId(token);
        return facilityService.getManagerFacilities(managerId);
    }

    @PostMapping
    public ResponseEntity<?> createFacility(@RequestBody FacilityRequest req,
                                             @RequestHeader("Authorization") String auth) {
        try {
            String token = auth.substring(7);
            String managerId = jwtUtil.extractUserId(token);
            String managerName = jwtUtil.extractName(token);
            ParkingFacility facility = facilityService.createFacility(req, managerId, managerName);
            return ResponseEntity.ok(facility);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFacility(@PathVariable String id,
                                             @RequestHeader("Authorization") String auth) {
        try {
            String token = auth.substring(7);
            String managerId = jwtUtil.extractUserId(token);
            facilityService.deleteFacility(id, managerId);
            return ResponseEntity.ok(Map.of("message", "Facility deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
