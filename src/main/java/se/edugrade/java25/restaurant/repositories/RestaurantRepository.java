package se.edugrade.java25.restaurant.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.java25.restaurant.model.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}
