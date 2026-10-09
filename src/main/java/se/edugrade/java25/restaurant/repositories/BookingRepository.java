package se.edugrade.java25.restaurant.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.java25.restaurant.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
