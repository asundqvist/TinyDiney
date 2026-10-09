package se.edugrade.java25.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RestaurantRequest(
        @NotBlank String name,
        @NotBlank String city,
        @NotBlank String cuisine,
        @Positive int seats
) {
}
