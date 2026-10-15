package se.edugrade.java25.restaurant.service;

import se.edugrade.java25.restaurant.dto.BookingResponse;

public interface NotificationService {
    void sendBookingConfirmation(BookingResponse booking);
}
