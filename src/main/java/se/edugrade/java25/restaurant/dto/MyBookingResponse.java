package se.edugrade.java25.restaurant.dto;

import se.edugrade.java25.restaurant.model.Booking;
import se.edugrade.java25.restaurant.model.BookingStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record MyBookingResponse(
        Long id,
        Long restaurantId,
        String customerName,
        String customerEmail,
        LocalDateTime time,
        int guests,
        BookingStatus status,
        OffsetDateTime cancellationDeadline,
        boolean cancellable,
        Integer staffUpdateRevision,
        String reference
) {
    public static MyBookingResponse from(Booking booking, Clock clock) {
        return new MyBookingResponse(
                booking.getId(),
                booking.getRestaurant().getId(),
                booking.getGuestName(),
                booking.getGuestEmail(),
                booking.getBookingTime(),
                booking.getGuestSize(),
                booking.getBookingStatus(),
                booking.guestCancellationDeadline(clock.getZone()),
                booking.canGuestCancel(clock),
                booking.getStaffUpdateRevision(),
                booking.getReference());
    }
}
