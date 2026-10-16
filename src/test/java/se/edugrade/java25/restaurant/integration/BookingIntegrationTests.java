package se.edugrade.java25.restaurant.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import se.edugrade.java25.restaurant.security.TestJwtConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import se.edugrade.java25.restaurant.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import se.edugrade.java25.restaurant.repositories.BookingRepository;
import se.edugrade.java25.restaurant.repositories.RestaurantRepository;
import java.time.LocalDateTime;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
class BookingIntegrationTests {
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

    @MockitoBean
    private NotificationService notificationService;


    @Test
    @DisplayName("Bokningen får rätt status, övergångslänkar, ETag och bekräftelseavisering")
    void bookingLifecycleIncludesConditionalGetAndLegalLinks() {
        long restaurantId = given().port(port).auth().oauth2(accessToken).contentType("application/json")
                .body(Map.of("name", "Bella", "city", "Stockholm", "cuisine", "Italian", "seats", 40))
                .post("/restaurants").then().statusCode(201).header("Location", containsString("/restaurants/"))
                .extract().jsonPath().getLong("id");

        long id = given().port(port).auth().oauth2(accessToken).contentType("application/json")
                .body(Map.of("restaurantId", restaurantId, "customerName", "Guest",
                        "customerEmail", "guest@example.com", "time", LocalDateTime.now().plusDays(1).toString(), "guests", 2))
                .post("/bookings").then().statusCode(201).header("Location", containsString("/bookings/"))
                .body("status", equalTo("REQUESTED"), "_links.confirm.href", notNullValue(),
                        "_links.cancel.href", notNullValue(), "_links.seat", nullValue())
                .extract().jsonPath().getLong("id");

        String etag = given().port(port).auth().oauth2(accessToken).get("/bookings/" + id).then().statusCode(200)
                .body("customerName", equalTo("Guest"), "guests", equalTo(2), "revision", nullValue())
                .header("ETag", notNullValue())
                .extract().header("ETag");
        given().port(port).auth().oauth2(accessToken).header("If-None-Match", etag).get("/bookings/" + id)
                .then().statusCode(304).body(isEmptyString());

        given().port(port).auth().oauth2(accessToken).post("/bookings/" + id + "/confirm").then().statusCode(200)
                .body("status", equalTo("CONFIRMED"), "_links.seat.href", notNullValue(), "_links.confirm", nullValue());
        verify(notificationService).sendBookingConfirmation(argThat(booking ->
                booking.id() == id && booking.status().name().equals("CONFIRMED")));
        given().port(port).auth().oauth2(accessToken).header("If-None-Match", etag).get("/bookings/" + id)
                .then().statusCode(200).header("ETag", not(equalTo(etag)));

        given().port(port).auth().oauth2(accessToken).post("/bookings/" + id + "/cancel").then().statusCode(200)
                .body("status", equalTo("CANCELLED"), "_links.cancel", nullValue(),
                        "_links.confirm", nullValue(), "_links.seat", nullValue());
        given().port(port).auth().oauth2(accessToken).post("/bookings/" + id + "/confirm").then().statusCode(409)
                .body("status", equalTo(409));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("En bekräftad bokning kan placeras och nekar därefter statusändringar")
    void confirmedBookingCanBeSeatedAndCannotChangeAfterwards() {
        long id = createBooking(createRestaurant());
        given().port(port).auth().oauth2(accessToken).post("/bookings/" + id + "/seat").then().statusCode(409);
        verifyNoInteractions(notificationService);
        given().port(port).auth().oauth2(accessToken).post("/bookings/" + id + "/confirm").then().statusCode(200);
        String etag = given().port(port).auth().oauth2(accessToken).get("/bookings/" + id).then().extract().header("ETag");
        given().port(port).auth().oauth2(accessToken).post("/bookings/" + id + "/seat").then().statusCode(200)
                .body("status", equalTo("SEATED"), "_links.confirm", nullValue(),
                        "_links.seat", nullValue(), "_links.cancel", nullValue());
        given().port(port).auth().oauth2(accessToken).header("If-None-Match", etag).get("/bookings/" + id)
                .then().statusCode(200).header("ETag", not(equalTo(etag)));
        for (String action : new String[]{"confirm", "seat", "cancel"}) {
            given().port(port).auth().oauth2(accessToken).post("/bookings/" + id + "/" + action).then().statusCode(409)
                    .contentType("application/problem+json").body("status", equalTo(409));
        }
        verify(notificationService).sendBookingConfirmation(argThat(booking -> booking.id() == id));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("En begärd bokning kan avbokas utan bekräftelseavisering")
    void requestedBookingCanBeCancelledWithoutNotification() {
        long id = createBooking(createRestaurant());
        given().port(port).auth().oauth2(accessToken).post("/bookings/" + id + "/cancel").then().statusCode(200)
                .body("status", equalTo("CANCELLED"), "_links.confirm", nullValue(),
                        "_links.seat", nullValue(), "_links.cancel", nullValue());
        for (String action : new String[]{"confirm", "seat", "cancel"}) {
            given().port(port).auth().oauth2(accessToken).post("/bookings/" + id + "/" + action).then().statusCode(409);
        }
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Ogiltiga bokningar ger 400 och okänd restaurang ger 404")
    void invalidBookingAndUnknownRestaurantCannotCreateBooking() {
        given().port(port).auth().oauth2(accessToken).contentType("application/json").body("{}")
                .post("/bookings").then().statusCode(400);
        given().port(port).auth().oauth2(accessToken).contentType("application/json")
                .body(bookingRequest(Long.MAX_VALUE, 2)).post("/bookings").then().statusCode(404)
                .contentType("application/problem+json").body("status", equalTo(404));
        given().port(port).auth().oauth2(accessToken).contentType("application/json")
                .body(bookingRequest(createRestaurant(), 0)).post("/bookings").then().statusCode(400);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Okänd bokning ger 404 som ProblemDetail")
    void unknownBookingReturnsProblemDetail() {
        given().port(port).auth().oauth2(accessToken).get("/bookings/9223372036854775807").then().statusCode(404)
                .contentType("application/problem+json").body("status", equalTo(404));
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
