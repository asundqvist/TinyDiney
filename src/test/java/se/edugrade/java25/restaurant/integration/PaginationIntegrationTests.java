package se.edugrade.java25.restaurant.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import se.edugrade.java25.restaurant.security.TestJwtConfiguration;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import se.edugrade.java25.restaurant.repositories.BookingRepository;
import se.edugrade.java25.restaurant.repositories.RestaurantRepository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
class PaginationIntegrationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private JwtEncoder jwtEncoder;

    private String accessToken;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @BeforeEach
    void clearDatabase() {
        accessToken = TestJwtConfiguration.token(jwtEncoder, "ADMIN");
        bookingRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    @Test
    @DisplayName("Restauranglistan delas upp i sidor utan överlapp")
    void restaurantPagesDoNotOverlap() {
        long first = createRestaurant();
        long second = createRestaurant();

        given().port(port).auth().oauth2(accessToken).get("/restaurants?page=0&size=1&sort=id,asc").then().statusCode(200)
                .body("content[0].id", equalTo((int) first), "content.size()", equalTo(1),
                        "page", equalTo(0), "size", equalTo(1), "totalElements", equalTo(2), "totalPages", equalTo(2));
        given().port(port).auth().oauth2(accessToken).get("/restaurants?page=1&size=1&sort=id,asc").then().statusCode(200)
                .body("content[0].id", equalTo((int) second), "page", equalTo(1));
        given().port(port).auth().oauth2(accessToken).get("/restaurants?page=2&size=1").then().statusCode(200)
                .body("content", empty(), "totalElements", equalTo(2));
    }

    @Test
    @DisplayName("Bokningslistan delas upp i sidor utan överlapp")
    void bookingPagesDoNotOverlap() {
        long restaurant = createRestaurant();
        long first = createBooking(restaurant);
        long second = createBooking(restaurant);

        given().port(port).auth().oauth2(accessToken).get("/bookings?page=0&size=1&sort=id,asc").then().statusCode(200)
                .body("content[0].id", equalTo((int) first), "content.size()", equalTo(1),
                        "page", equalTo(0), "size", equalTo(1), "totalElements", equalTo(2), "totalPages", equalTo(2));
        given().port(port).auth().oauth2(accessToken).get("/bookings?page=1&size=1&sort=id,asc").then().statusCode(200)
                .body("content[0].id", equalTo((int) second), "page", equalTo(1));
        given().port(port).auth().oauth2(accessToken).get("/bookings?page=2&size=1").then().statusCode(200)
                .body("content", empty(), "totalElements", equalTo(2));
    }
    private long createRestaurant() {
        return given().port(port).auth().oauth2(accessToken).contentType("application/json")
                .body(Map.of("name", "Bella", "city", "Stockholm", "cuisine", "Italian", "seats", 40))
                .post("/restaurants").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    private long createBooking(long restaurantId) {
        return given().port(port).auth().oauth2(accessToken).contentType("application/json").body(bookingRequest(restaurantId, 2))
                .post("/bookings").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    private Map<String, Object> bookingRequest(long restaurantId, int guests) {
        return Map.of("restaurantId", restaurantId, "customerName", "Guest",
                "customerEmail", "guest@example.com", "time", LocalDateTime.now().plusDays(1).toString(), "guests", guests);
    }
}
