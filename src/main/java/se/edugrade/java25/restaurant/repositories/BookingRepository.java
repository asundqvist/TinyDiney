package se.edugrade.java25.restaurant.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import se.edugrade.java25.restaurant.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByReferenceIsNull(Pageable pageable);

    Page<Booking> findByOwnerSubject(String ownerSubject, Pageable pageable);
}
