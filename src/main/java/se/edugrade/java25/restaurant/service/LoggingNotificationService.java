package se.edugrade.java25.restaurant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.edugrade.java25.restaurant.dto.BookingResponse;

@Slf4j
@Service
public class LoggingNotificationService implements NotificationService {
    @Override
    public void sendBookingConfirmation(BookingResponse booking) {
        log.info("Booking confirmation notification (simulated) for booking {}", booking.id());
    }
}
