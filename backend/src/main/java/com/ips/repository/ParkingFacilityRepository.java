package com.ips.repository;

import com.ips.model.ParkingFacility;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ParkingFacilityRepository extends MongoRepository<ParkingFacility, String> {
    List<ParkingFacility> findByManagerId(String managerId);
    List<ParkingFacility> findByCityContainingIgnoreCaseAndIsActiveTrue(String city);
    List<ParkingFacility> findByIsActiveTrue();
}
