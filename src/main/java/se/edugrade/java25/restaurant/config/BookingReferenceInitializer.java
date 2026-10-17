package se.edugrade.java25.restaurant.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.java25.restaurant.model.Booking;
import se.edugrade.java25.restaurant.repositories.BookingRepository;

@Component
@RequiredArgsConstructor
public class BookingReferenceInitializer implements ApplicationRunner {
    private final BookingRepository bookings;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        while (true) {
            var batch = bookings.findByReferenceIsNull(PageRequest.of(0, 100)).getContent();
            if (batch.isEmpty()) return;
            batch.forEach(Booking::ensureReference);
            bookings.saveAllAndFlush(batch);
        }
    }
}
