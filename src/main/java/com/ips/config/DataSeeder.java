package com.ips.config;

import com.ips.dto.FacilityRequest;
import com.ips.model.User;
import com.ips.model.enums.Role;
import com.ips.repository.ParkingFacilityRepository;
import com.ips.repository.UserRepository;
import com.ips.service.FacilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Seeds the database with initial test data if it's empty.
 * Creates a default manager and 4 city-wise parking facilities.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private ParkingFacilityRepository facilityRepository;
    @Autowired private FacilityService facilityService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (facilityRepository.count() == 0) {
            System.out.println("No facilities found. Seeding database with initial data...");

            // 1. Create a default Manager
            User adminManager = userRepository.findByEmail("admin@ips.com").orElseGet(() -> {
                User manager = User.builder()
                        .name("Admin Manager")
                        .email("admin@ips.com")
                        .password(passwordEncoder.encode("admin123"))
                        .phone("9876543210")
                        .role(Role.MANAGER)
                        .createdAt(LocalDateTime.now())
                        .build();
                return userRepository.save(manager);
            });

            // 2. Create Facilities across different cities
            seedFacility(adminManager, "Mumbai", "Mumbai Central Parking", "Plot 42, near Metro", "Maharashtra", 40,
                    15, 10, 5, 2, 8, "MULTI_LEVEL", "CCTV, SECURITY, WASHROOM, ELEVATOR");

            seedFacility(adminManager, "Delhi", "Connaught Place Hub", "CP Inner Circle", "Delhi", 30,
                    10, 10, 5, 2, 3, "UNDERGROUND", "CCTV, VALET, CAR_WASH");

            seedFacility(adminManager, "Bangalore", "Tech Park Parking", "Whitefield Main Rd", "Karnataka", 50,
                    20, 15, 8, 3, 4, "MULTI_LEVEL", "CCTV, EV_FAST_CHARGE, CAFETERIA");

            seedFacility(adminManager, "Hyderabad", "Hitec City Open Air", "Mindspace Junction", "Telangana", 25,
                    0, 0, 2, 1, 22, "OPEN_AIR", "SECURITY, FOOD_TRUCK_ZONE");

            System.out.println("Database seeding completed successfully!");
        }
    }

    private void seedFacility(User manager, String city, String name, String address, String state,
                              int price, int std, int covered, int ev, int hc, int open,
                              String type, String amenitiesStr) {
        
        FacilityRequest req = new FacilityRequest();
        req.setName(name);
        req.setAddress(address);
        req.setCity(city);
        req.setState(state);
        req.setPricePerHour(price);
        req.setStandardSlots(std);
        req.setCoveredSlots(covered);
        req.setEvChargingSlots(ev);
        req.setHandicapSlots(hc);
        req.setOpenAirSlots(open);
        req.setTotalSlots(std + covered + ev + hc + open);
        req.setFacilityType(type);
        req.setAmenities(Arrays.asList(amenitiesStr.split(", ")));
        req.setOperatingHours("24/7");

        facilityService.createFacility(req, manager.getId(), manager.getName());
    }
}
