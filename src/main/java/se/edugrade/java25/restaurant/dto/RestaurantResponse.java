package se.edugrade.java25.restaurant.dto;

public record RestaurantResponse(
        Long id,
        String name,
        String city,
        String cuisine,
        int seats
) {
}
