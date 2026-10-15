package se.edugrade.java25.restaurant.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import se.edugrade.java25.restaurant.repositories.BookingRepository;
import se.edugrade.java25.restaurant.repositories.RestaurantRepository;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Restauranger")
class RestaurantIntegrationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @BeforeEach
    void clearDatabase() {
        bookingRepository.deleteAll();
        restaurantRepository.deleteAll();
    }


    @Test
    @DisplayName("En restaurang kan uppdateras och ändringarna sparas")
    void restaurantCanBeUpdated() {
        long first = createRestaurant();
        given().port(port).contentType("application/json")
                .body(Map.of("name", "Updated", "city", "Uppsala", "cuisine", "Swedish", "seats", 50))
                .put("/restaurants/" + first).then().statusCode(200)
                .body("id", equalTo((int) first), "seats", equalTo(50));
        given().port(port).get("/restaurants/" + first).then().statusCode(200)
                .body("name", equalTo("Updated"), "city", equalTo("Uppsala"), "seats", equalTo(50));
    }

    @Test
    @DisplayName("Okänd restaurang ger 404 och ogiltiga uppgifter ger 400")
    void unknownRestaurantAndInvalidInputReturnErrors() {
        given().port(port).get("/restaurants/9223372036854775807").then().statusCode(404)
                .contentType("application/problem+json").body("status", equalTo(404));
        given().port(port).contentType("application/json").body("{}")
                .post("/restaurants").then().statusCode(400);
    }
    private long createRestaurant() {
        return given().port(port).contentType("application/json")
                .body(Map.of("name", "Bella", "city", "Stockholm", "cuisine", "Italian", "seats", 40))
                .post("/restaurants").then().statusCode(201).extract().jsonPath().getLong("id");
    }
}
