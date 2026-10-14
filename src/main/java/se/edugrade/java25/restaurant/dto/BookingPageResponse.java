package se.edugrade.java25.restaurant.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record BookingPageResponse(
        List<BookingResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static BookingPageResponse from(Page<BookingResponse> page) {
        return new BookingPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
