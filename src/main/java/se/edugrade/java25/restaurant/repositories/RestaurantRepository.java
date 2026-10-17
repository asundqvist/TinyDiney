package se.edugrade.java25.restaurant.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.java25.restaurant.model.Restaurant;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByDemoKey(String demoKey);
    Optional<Restaurant> findByNameAndCity(String name, String city);
}
