package se.edugrade.java25.restaurant.dto;

import se.edugrade.java25.restaurant.model.Booking;
import se.edugrade.java25.restaurant.model.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long restaurantId,
        String customerName,
        String customerEmail,
        LocalDateTime time,
        int guests,
        BookingStatus status
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getRestaurant().getId(),
                booking.getGuestName(),
                booking.getGuestEmail(),
                booking.getBookingTime(),
                booking.getGuestSize(),
                booking.getBookingStatus());
    }
}
