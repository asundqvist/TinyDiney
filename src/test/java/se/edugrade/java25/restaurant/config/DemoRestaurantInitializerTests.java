package se.edugrade.java25.restaurant.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import se.edugrade.java25.restaurant.model.Restaurant;
import se.edugrade.java25.restaurant.repositories.RestaurantRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DemoRestaurantInitializerTests {
    private DemoRestaurantInitializer initializer(List<Restaurant> rows) {
        RestaurantRepository repository = mock(RestaurantRepository.class);
        when(repository.count()).thenAnswer(call -> (long) rows.size());
        when(repository.findByDemoKey(anyString())).thenAnswer(call -> rows.stream()
                .filter(row -> Objects.equals(row.getDemoKey(), call.getArgument(0))).findFirst());
        when(repository.findByNameAndCity(anyString(), anyString())).thenAnswer(call -> rows.stream()
                .filter(row -> row.getName().equals(call.getArgument(0)) && row.getCity().equals(call.getArgument(1))).findFirst());
        when(repository.save(any(Restaurant.class))).thenAnswer(call -> {
            Restaurant row = call.getArgument(0);
            if (!rows.contains(row)) rows.add(row);
            return row;
        });
        var initializer = new DemoRestaurantInitializer(repository);
        ReflectionTestUtils.setField(initializer, "catalogue", new ClassPathResource("demo/restaurants.json"));
        return initializer;
    }

    @Test
    void expandsExistingDatabaseOnceAndPreservesLegacyIdsAndCapacity() throws Exception {
        var pudding = new Restaurant("Golden Cub Diner", "Uppsala", "Diner favourites", 99);
        var burger = new Restaurant("Little Harbour", "Gothenburg", "Fish", 37);
        ReflectionTestUtils.setField(pudding, "id", 51L);
        ReflectionTestUtils.setField(burger, "id", 52L);
        List<Restaurant> rows = new ArrayList<>(List.of(pudding, burger));
        var initializer = initializer(rows);
        initializer.run(null);
        initializer.run(null);
        assertEquals("Puddington", pudding.getName());
        assertEquals(51L, pudding.getId());
        assertEquals(99, pudding.getSeats());
        assertEquals("puddington", pudding.getDemoKey());
        assertEquals("Little Burger Barn", burger.getName());
        assertEquals(52L, burger.getId());
        assertEquals(37, burger.getSeats());
        assertEquals(8, rows.size(), "Only the six new demos are added to an existing database");
        assertEquals(8, rows.stream().map(Restaurant::getDemoKey).distinct().count());
    }

    @Test
    void seedsTwelveOnceAndNeverRecreatesAdminRenamedRestaurants() throws Exception {
        List<Restaurant> rows = new ArrayList<>();
        var initializer = initializer(rows);
        initializer.run(null);
        assertEquals(12, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.getSeats() > 0 && row.getDemoKey() != null));
        var original = rows.stream().filter(row -> row.getDemoKey().equals("maple-mini")).findFirst().orElseThrow();
        var expansion = rows.stream().filter(row -> row.getDemoKey().equals("noodle-nest")).findFirst().orElseThrow();
        original.update("Admin's cafe", "New city", "New cuisine", 77);
        expansion.update("Admin's pasta", "Another city", "Another cuisine", 33);
        initializer.run(null);
        assertEquals(12, rows.size());
        assertEquals("Admin's cafe", original.getName());
        assertEquals("New city", original.getCity());
        assertEquals(77, original.getSeats());
        assertEquals("Admin's pasta", expansion.getName());
        assertEquals(33, expansion.getSeats());
        assertEquals(12, rows.stream().map(Restaurant::getDemoKey).distinct().count());
    }
}
