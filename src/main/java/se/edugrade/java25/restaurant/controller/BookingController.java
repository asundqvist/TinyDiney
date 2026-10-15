package se.edugrade.java25.restaurant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.edugrade.java25.restaurant.dto.BookingRequest;
import se.edugrade.java25.restaurant.dto.BookingPageResponse;
import se.edugrade.java25.restaurant.dto.BookingResponse;
import se.edugrade.java25.restaurant.dto.BookingResult;
import se.edugrade.java25.restaurant.model.Booking;
import se.edugrade.java25.restaurant.service.BookingService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;


    @GetMapping
    public BookingPageResponse findAll(Pageable pageable) {
        return bookingService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<BookingResponse>> findById(@PathVariable Long id, WebRequest req) {
        BookingResult result = bookingService.findByIdWithRevision(id);
        String tag = BookingETag.of(id, result.revision());

        if (req.checkNotModified(tag)) {
            return null;
        }

            return ResponseEntity.ok()
                    .eTag(tag)
                    .body(toModel(result.response()));
    }

    @PostMapping
    public ResponseEntity<EntityModel<BookingResponse>> create(@Valid @RequestBody BookingRequest req) {
        BookingResponse response = bookingService.create(req.restaurantId(), req);

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(toModel(response));
    }

    @PostMapping("/{id}/confirm")
    public EntityModel<BookingResponse> confirm(@PathVariable Long id) {
        return toModel(bookingService.confirm(id));
    }

    @PostMapping("/{id}/seat")
    public EntityModel<BookingResponse> seat(@PathVariable Long id) {
        return toModel(bookingService.seat(id));
    }

    @PostMapping("/{id}/cancel")
    public EntityModel<BookingResponse> cancel(@PathVariable Long id) {
        return toModel(bookingService.cancel(id));
    }

    private EntityModel<BookingResponse> toModel(BookingResponse response) {
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/bookings/{id}")
                .buildAndExpand(response.id())
                .toUriString();

        EntityModel<BookingResponse> model = EntityModel.of(response, Link.of(url).withSelfRel());

        switch (response.status()) {
            case REQUESTED -> model.add(Link.of(url + "/confirm", "confirm"), Link.of(url + "/cancel", "cancel"));
            case CONFIRMED -> model.add(Link.of(url + "/seat", "seat"), Link.of(url + "/cancel", "cancel"));
            case SEATED, CANCELLED -> { }
        }
        return model;
    }
}
