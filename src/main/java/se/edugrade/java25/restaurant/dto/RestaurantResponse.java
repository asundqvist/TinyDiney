package se.edugrade.java25.restaurant.dto;

import se.edugrade.java25.restaurant.model.Restaurant;

public record RestaurantResponse(
        Long id,
        String name,
        String city,
        String cuisine,
        int seats,
        String demoKey
) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCity(),
                restaurant.getCuisine(),
                restaurant.getSeats(),
                restaurant.getDemoKey());
    }
}
