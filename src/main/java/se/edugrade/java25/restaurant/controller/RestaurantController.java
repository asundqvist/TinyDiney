package se.edugrade.java25.restaurant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.edugrade.java25.restaurant.api.RestaurantApi;
import se.edugrade.java25.restaurant.dto.RestaurantRequest;
import se.edugrade.java25.restaurant.dto.RestaurantPageResponse;
import se.edugrade.java25.restaurant.dto.RestaurantResponse;
import se.edugrade.java25.restaurant.service.RestaurantService;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController implements RestaurantApi {
    private final RestaurantService restaurantService;

    @GetMapping
    public RestaurantPageResponse findAll(@ParameterObject Pageable pageable) {
        return restaurantService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public RestaurantResponse findById(@PathVariable Long id) {
        return restaurantService.findById(id);
    }

    @PostMapping
    public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody RestaurantRequest req) {
        RestaurantResponse response = restaurantService.create(req);

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public RestaurantResponse update(@PathVariable Long id, @Valid @RequestBody RestaurantRequest req) {
        return restaurantService.update(id, req);
    }
}
