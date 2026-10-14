package se.edugrade.java25.restaurant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.java25.restaurant.dto.BookingRequest;
import se.edugrade.java25.restaurant.dto.BookingPageResponse;
import se.edugrade.java25.restaurant.dto.BookingResponse;
import se.edugrade.java25.restaurant.exceptions.ResourceNotFoundException;
import se.edugrade.java25.restaurant.model.Booking;
import se.edugrade.java25.restaurant.model.Restaurant;
import se.edugrade.java25.restaurant.repositories.BookingRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final RestaurantService restaurantService;

    //SonarQube made me do it...
    private static final String BOOKING_STATUS_CHANGED = "Booking {} changed to {}";

    public BookingPageResponse findAll(Pageable pageable) {
        return BookingPageResponse.from(bookingRepository
                .findAll(pageable)
                .map(BookingResponse::from)
        );
    }

    public BookingResponse findById(Long id) {
        return BookingResponse.from(getBookingByIdOrThrow(id));
    }

    @Transactional
    public BookingResponse create(Long restaurantId, BookingRequest req) {
        Restaurant restaurant = restaurantService.getRestaurantByIdOrThrow(restaurantId);

        Booking booking = new Booking(restaurant,
                req.customerName(),
                req.customerEmail(),
                req.time(),
                req.guests());

        Booking saved = bookingRepository.save(booking);
        log.info("Created booking {} for restaurant {} with status {}",
                saved.getId(), saved.getRestaurant(), saved.getBookingStatus());
        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse confirm(Long id) {
        Booking booking = getBookingByIdOrThrow(id);
        booking.confirm();

        BookingResponse confirmed = BookingResponse.from(bookingRepository.save(booking));
        notificationService.sendBookingConfirmation(confirmed);
        log.info(BOOKING_STATUS_CHANGED, confirmed.id(), confirmed.status());
        return confirmed;
    }

    @Transactional
    public BookingResponse seat(Long id) {
        Booking booking = getBookingByIdOrThrow(id);
        booking.seat();

        BookingResponse seated = BookingResponse.from(bookingRepository.save(booking));
        log.info(BOOKING_STATUS_CHANGED, seated.id(), seated.status());
        return seated;
    }

    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = getBookingByIdOrThrow(id);
        booking.cancel();

        BookingResponse canceled = BookingResponse.from(bookingRepository.save(booking));
        log.info(BOOKING_STATUS_CHANGED, canceled.id(), canceled.status());
        return canceled;
    }

    private Booking getBookingByIdOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking with id [" + id + "] not found"));
    }
}
