package se.edugrade.java25.restaurant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.java25.restaurant.dto.RestaurantPageResponse;
import se.edugrade.java25.restaurant.dto.RestaurantRequest;
import se.edugrade.java25.restaurant.dto.RestaurantResponse;
import se.edugrade.java25.restaurant.exceptions.ResourceNotFoundException;
import se.edugrade.java25.restaurant.model.Restaurant;
import se.edugrade.java25.restaurant.repositories.RestaurantRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;

    public RestaurantPageResponse findAll(Pageable pageable) {
        return RestaurantPageResponse.from(restaurantRepository
                .findAll(pageable)
                .map(RestaurantResponse::from)
        );
    }

    public RestaurantResponse findById(Long id) {
        return RestaurantResponse.from(getRestaurantByIdOrThrow(id));
    }

    @Transactional
    public RestaurantResponse create(RestaurantRequest req) {
        Restaurant restaurant = new Restaurant(
                req.name(),
                req.city(),
                req.cuisine(),
                req.seats());

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Created restaurant: {} (id={})", saved.getName(), saved.getId());
        return RestaurantResponse.from(saved);
    }

    @Transactional
    public RestaurantResponse update(Long id, RestaurantRequest req) {

        Restaurant restaurant = getRestaurantByIdOrThrow(id);
        restaurant.update(
                req.name(),
                req.city(),
                req.cuisine(),
                req.seats());

        log.info("Updated restaurant: {} (id={})", restaurant.getName(), restaurant.getId());
        return RestaurantResponse.from(restaurantRepository.save(restaurant));
    }

    public Restaurant getRestaurantByIdOrThrow(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant with id [" + id + "] not found"));
    }
}
