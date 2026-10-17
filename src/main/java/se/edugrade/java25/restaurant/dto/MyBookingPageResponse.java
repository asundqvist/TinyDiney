package se.edugrade.java25.restaurant.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record MyBookingPageResponse(
        List<MyBookingResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static MyBookingPageResponse from(Page<MyBookingResponse> page) {
        return new MyBookingPageResponse(page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
