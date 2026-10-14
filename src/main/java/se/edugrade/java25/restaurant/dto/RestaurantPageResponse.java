package se.edugrade.java25.restaurant.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record RestaurantPageResponse(
        List<RestaurantResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static RestaurantPageResponse from(Page<RestaurantResponse> page) {
        return new RestaurantPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}