package se.edugrade.java25.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import se.edugrade.java25.restaurant.model.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long restaurantId,
        String customerName,
        String customerEmail,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        LocalDateTime time,

        int guests,
        BookingStatus status
) {
}
