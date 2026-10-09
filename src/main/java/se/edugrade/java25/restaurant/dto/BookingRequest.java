package se.edugrade.java25.restaurant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record BookingRequest(
        @NotNull
        Long restaurantId,

        @NotBlank(message = "Name is required")
        String customerName,

        @NotBlank(message = "Email is required")
        @Email String customerEmail,

        @NotNull(message = "Booking time is required")
        @Future LocalDateTime time,

        @NotNull(message = "Number of guests must be above 0")
        int guests
) {
}
