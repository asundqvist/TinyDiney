package se.edugrade.java25.restaurant.dto;

import jakarta.validation.constraints.*;

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

        @Min(value = 1, message = "Number of guests must be above 0")
        @Max(value = 10, message = "Number of guests can't be above 10")
        int guests
) {
}
