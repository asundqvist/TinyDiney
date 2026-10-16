package se.edugrade.java25.restaurant;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import se.edugrade.java25.restaurant.security.TestJwtConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the whole application context. It fails if a bean cannot be created,
 * which catches most wiring mistakes before you ever open a browser.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@DisplayName("Applikationens uppstart")
class RestaurantApiApplicationTests {

    @Test
    @DisplayName("Applikationen startar med testprofilen")
    void contextLoads() {
    }
}
