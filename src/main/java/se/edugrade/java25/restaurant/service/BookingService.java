package se.edugrade.java25.restaurant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.java25.restaurant.dto.BookingRequest;
import se.edugrade.java25.restaurant.dto.BookingPageResponse;
import se.edugrade.java25.restaurant.dto.BookingResponse;
import se.edugrade.java25.restaurant.dto.BookingResult;
import se.edugrade.java25.restaurant.dto.MyBookingPageResponse;
import se.edugrade.java25.restaurant.dto.MyBookingResponse;
import se.edugrade.java25.restaurant.exceptions.InvalidBookingStateException;
import se.edugrade.java25.restaurant.exceptions.ResourceNotFoundException;
import se.edugrade.java25.restaurant.model.Booking;
import se.edugrade.java25.restaurant.model.Restaurant;
import se.edugrade.java25.restaurant.repositories.BookingRepository;

import java.time.Clock;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final RestaurantService restaurantService;
    private final BookingAccess access;
    private final Clock clock;

    //SonarQube made me do it...
    private static final String BOOKING_STATUS_CHANGED = "Booking {} changed to {}";

    @Transactional(readOnly = true)
    public BookingPageResponse findAll(Pageable pageable) {
        var bookings = access.isStaff() ? bookingRepository.findAll(pageable)
                : bookingRepository.findByOwnerSubject(access.subject(), pageable);
        return BookingPageResponse.from(bookings.map(BookingResponse::from));
    }

    @Transactional(readOnly = true)
    public MyBookingPageResponse findMine(Pageable pageable) {
        return MyBookingPageResponse.from(bookingRepository.findByOwnerSubject(access.subject(), pageable)
                .map(booking -> MyBookingResponse.from(booking, clock)));
    }

    public BookingResponse findById(Long id) {
        return BookingResponse.from(getVisibleBooking(id));
    }

    @Transactional
    public BookingResponse create(Long restaurantId, BookingRequest req) {
        Restaurant restaurant = restaurantService.getRestaurantByIdOrThrow(restaurantId);

        Booking booking = new Booking(restaurant,
                req.customerName(),
                req.customerEmail(),
                req.time(),
                req.guests(), access.subject());

        Booking saved = bookingRepository.save(booking);
        log.info("Created booking {} for restaurant {} with status {}",
                saved.getId(), saved.getRestaurant(), saved.getBookingStatus());
        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse confirm(Long id) {
        Booking booking = getBookingByIdOrThrow(id);
        booking.confirm();
        booking.markHandledByStaff();

        BookingResponse confirmed = BookingResponse.from(bookingRepository.saveAndFlush(booking));

        notificationService.sendBookingConfirmation(confirmed);

        log.info(BOOKING_STATUS_CHANGED, confirmed.id(), confirmed.status());
        return confirmed;
    }

    @Transactional
    public BookingResponse seat(Long id) {
        Booking booking = getBookingByIdOrThrow(id);
        booking.seat();
        booking.markHandledByStaff();

        BookingResponse seated = BookingResponse.from(bookingRepository.saveAndFlush(booking));
        log.info(BOOKING_STATUS_CHANGED, seated.id(), seated.status());
        return seated;
    }

    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = getVisibleBooking(id);
        if (!access.isStaff() && !booking.canGuestCancel(clock)) {
            throw new InvalidBookingStateException("Guest cancellation is only available for pending or confirmed bookings until one hour before the booking time.");
        }
        if (access.isStaff()) {
            booking.cancel();
            booking.markHandledByStaff();
        } else {
            booking.cancelByGuest();
        }

        BookingResponse canceled = BookingResponse.from(bookingRepository.saveAndFlush(booking));
        log.info(BOOKING_STATUS_CHANGED, canceled.id(), canceled.status());
        return canceled;
    }

    public BookingResult findByIdWithRevision(Long id) {
        Booking booking = getVisibleBooking(id);

        return new BookingResult(
                BookingResponse.from(booking),
                booking.getRevision()
        );
    }

    private Booking getVisibleBooking(Long id) {
        Booking booking = getBookingByIdOrThrow(id);
        if (!access.isStaff() && !access.subject().equals(booking.getOwnerSubject())) {
            // Other guests' bookings and unknown IDs are deliberately indistinguishable.
            throw new ResourceNotFoundException("Booking with id [" + id + "] not found");
        }
        return booking;
    }

    private Booking getBookingByIdOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking with id [" + id + "] not found"));
    }
}
