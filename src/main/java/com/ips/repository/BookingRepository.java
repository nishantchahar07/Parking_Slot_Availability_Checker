package com.ips.repository;

import com.ips.model.Booking;
import com.ips.model.enums.BookingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByDriverIdOrderByCreatedAtDesc(String driverId);
    List<Booking> findByFacilityIdOrderByCreatedAtDesc(String facilityId);
    List<Booking> findByDriverIdAndStatus(String driverId, BookingStatus status);
}
