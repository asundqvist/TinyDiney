package se.edugrade.java25.restaurant.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import se.edugrade.java25.restaurant.security.TestJwtConfiguration;
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
@Import(TestJwtConfiguration.class)
@DisplayName("Restauranger")
class RestaurantIntegrationTests {
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
    @DisplayName("En restaurang kan uppdateras och ändringarna sparas")
    void restaurantCanBeUpdated() {
        long first = createRestaurant();
        given().port(port).auth().oauth2(accessToken).contentType("application/json")
                .body(Map.of("name", "Updated", "city", "Uppsala", "cuisine", "Swedish", "seats", 50))
                .put("/restaurants/" + first).then().statusCode(200)
                .body("id", equalTo((int) first), "seats", equalTo(50));
        given().port(port).auth().oauth2(accessToken).get("/restaurants/" + first).then().statusCode(200)
                .body("name", equalTo("Updated"), "city", equalTo("Uppsala"), "seats", equalTo(50));
    }

    @Test
    @DisplayName("Okänd restaurang ger 404 och ogiltiga uppgifter ger 400")
    void unknownRestaurantAndInvalidInputReturnErrors() {
        given().port(port).auth().oauth2(accessToken).get("/restaurants/9223372036854775807").then().statusCode(404)
                .contentType("application/problem+json").body("status", equalTo(404));
        given().port(port).auth().oauth2(accessToken).contentType("application/json").body("{}")
                .post("/restaurants").then().statusCode(400);
    }
    @Test
    void demoExpansionIsPaginatedAndAdminEditsKeepTheirStableIdentity() throws Exception {
        var initializer = new se.edugrade.java25.restaurant.config.DemoRestaurantInitializer(restaurantRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(initializer, "catalogue",
                new org.springframework.core.io.ClassPathResource("demo/restaurants.json"));
        initializer.run(null);
        var first = given().port(port).auth().oauth2(accessToken).get("/restaurants?size=6&page=0&sort=id,asc")
                .then().statusCode(200).body("totalElements", equalTo(12), "totalPages", equalTo(2), "content.size()", equalTo(6))
                .extract().jsonPath().getList("content.id", Long.class);
        var second = given().port(port).auth().oauth2(accessToken).get("/restaurants?size=6&page=1&sort=id,asc")
                .then().statusCode(200).body("content.size()", equalTo(6))
                .extract().jsonPath().getList("content.id", Long.class);
        var ids = new java.util.HashSet<>(first);
        ids.addAll(second);
        org.junit.jupiter.api.Assertions.assertEquals(12, ids.size());
        var noodle = restaurantRepository.findByDemoKey("noodle-nest").orElseThrow();
        given().port(port).auth().oauth2(accessToken).contentType("application/json")
                .body(Map.of("name", "Renamed Noodle Nest", "city", "New city", "cuisine", "New cuisine", "seats", 29))
                .put("/restaurants/" + noodle.getId()).then().statusCode(200)
                .body("demoKey", equalTo("noodle-nest"), "name", equalTo("Renamed Noodle Nest"));
        initializer.run(null);
        org.junit.jupiter.api.Assertions.assertEquals(12, restaurantRepository.count());
        given().port(port).auth().oauth2(accessToken).get("/restaurants/" + noodle.getId()).then().statusCode(200)
                .body("demoKey", equalTo("noodle-nest"), "name", equalTo("Renamed Noodle Nest"), "seats", equalTo(29));
    }

    private long createRestaurant() {
        return given().port(port).auth().oauth2(accessToken).contentType("application/json")
                .body(Map.of("name", "Bella", "city", "Stockholm", "cuisine", "Italian", "seats", 40))
                .post("/restaurants").then().statusCode(201).extract().jsonPath().getLong("id");
    }
}
