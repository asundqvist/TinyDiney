package se.edugrade.java25.restaurant.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.doReturn;
import se.edugrade.java25.restaurant.model.Booking;
import se.edugrade.java25.restaurant.model.Restaurant;
import se.edugrade.java25.restaurant.repositories.BookingRepository;
import se.edugrade.java25.restaurant.repositories.RestaurantRepository;

import java.time.*;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestJwtConfiguration.class, MyBookingIntegrationTests.FixedBookingClock.class})
class MyBookingIntegrationTests {
    @TestConfiguration
    static class FixedBookingClock {
        @Bean
        @Primary
        Clock fixedBookingClock() {
            return Clock.fixed(Instant.parse("2099-06-01T12:00:00Z"), ZoneId.of("Europe/Stockholm"));
        }
    }

    @LocalServerPort private int port;
    @Autowired private JwtEncoder encoder;
    @Autowired private BookingRepository bookings;
    @Autowired private RestaurantRepository restaurants;
    @MockitoSpyBean private Clock clock;
    private Restaurant restaurant;
    private String alice;
    private String bob;
    private String staff;

    @BeforeEach
    void setUp() {
        bookings.deleteAll();
        restaurants.deleteAll();
        restaurant = restaurants.save(new Restaurant("Little Fork", "Stockholm", "Family food", 40));
        alice = token("auth0|alice", "GUEST", TestJwtConfiguration.ALL_SCOPES);
        bob = token("auth0|bob", "GUEST", TestJwtConfiguration.ALL_SCOPES);
        staff = token("auth0|staff", "STAFF", TestJwtConfiguration.ALL_SCOPES);
    }

    @Test
    void guestsOnlySeeTheirOwnBookingsEvenWhenContactEmailsAreIdentical() {
        long aliceId = create(alice, LocalDateTime.now(clock).plusHours(2));
        long bobId = create(bob, LocalDateTime.now(clock).plusHours(3));
        given().port(port).auth().oauth2(alice).get("/bookings/mine").then().statusCode(200)
                .body("totalElements", equalTo(1), "content[0].id", equalTo((int) aliceId),
                        "content[0].cancellable", equalTo(true), "content[0].cancellationDeadline", notNullValue(),
                        "content[0].ownerSubject", nullValue());
        given().port(port).auth().oauth2(alice).get("/bookings").then().statusCode(200)
                .body("totalElements", equalTo(1), "content[0].id", equalTo((int) aliceId));
        given().port(port).auth().oauth2(bob).get("/bookings/mine").then().statusCode(200)
                .body("totalElements", equalTo(1), "content[0].id", equalTo((int) bobId));
        given().port(port).auth().oauth2(alice).get("/bookings/" + bobId).then().statusCode(404);
        given().port(port).auth().oauth2(alice).post("/bookings/" + bobId + "/cancel").then().statusCode(404);
        given().port(port).auth().oauth2(staff).get("/bookings").then().statusCode(200)
                .body("totalElements", equalTo(2));
        given().port(port).auth().oauth2(staff).get("/bookings/mine").then().statusCode(200)
                .body("totalElements", equalTo(0));
        assertEquals("REQUESTED", bookings.findById(bobId).orElseThrow().getBookingStatus().name());
    }

    @Test
    void guestCanCancelOwnPendingAndConfirmedBookings() {
        long pending = create(alice, LocalDateTime.now(clock).plusHours(2));
        given().port(port).auth().oauth2(alice).post("/bookings/" + pending + "/cancel").then().statusCode(200)
                .body("status", equalTo("CANCELLED"), "guestCancelledAfterConfirmation", equalTo(false));
        assertEquals(null, bookings.findById(pending).orElseThrow().getStaffUpdateRevision());
        long confirmed = create(alice, LocalDateTime.now(clock).plusHours(3));
        given().port(port).auth().oauth2(staff).post("/bookings/" + confirmed + "/confirm").then().statusCode(200);
        given().port(port).auth().oauth2(alice).get("/bookings/mine").then().statusCode(200)
                .body("content.find { it.id == " + confirmed + " }.staffUpdateRevision", equalTo(2));
        given().port(port).auth().oauth2(alice).post("/bookings/" + confirmed + "/cancel").then().statusCode(200)
                .body("status", equalTo("CANCELLED"), "guestCancelledAfterConfirmation", equalTo(true));
        given().port(port).auth().oauth2(staff).get("/bookings").then().statusCode(200)
                .body("content.find { it.id == " + confirmed + " }.guestCancelledAfterConfirmation", equalTo(true),
                        "content.find { it.id == " + pending + " }.guestCancelledAfterConfirmation", equalTo(false));
        assertEquals(2, bookings.findById(confirmed).orElseThrow().getStaffUpdateRevision());
        given().port(port).auth().oauth2(alice).post("/bookings/" + confirmed + "/cancel").then().statusCode(409);
    }

    @Test
    void staffCancellationsDoNotRaiseGuestAlertsAndStaffCanSeatConfirmedBookings() {
        long cancelled = create(alice, LocalDateTime.now(clock).plusHours(3));
        given().port(port).auth().oauth2(staff).post("/bookings/" + cancelled + "/confirm").then().statusCode(200);
        given().port(port).auth().oauth2(staff).post("/bookings/" + cancelled + "/cancel").then().statusCode(200)
                .body("guestCancelledAfterConfirmation", equalTo(false));
        long seated = create(alice, LocalDateTime.now(clock).plusHours(4));
        given().port(port).auth().oauth2(staff).post("/bookings/" + seated + "/confirm").then().statusCode(200);
        given().port(port).auth().oauth2(alice).post("/bookings/" + seated + "/seat").then().statusCode(403);
        given().port(port).auth().oauth2(staff).post("/bookings/" + seated + "/seat").then().statusCode(200)
                .body("status", equalTo("SEATED"));
        given().port(port).auth().oauth2(staff).get("/bookings").then().statusCode(200)
                .body("content.find { it.id == " + seated + " }.status", equalTo("SEATED"));
    }

    @Test
    void cancellationIsAllowedExactlyOneHourBeforeButRejectedInsideTheLastHour() {
        long boundary = create(alice, LocalDateTime.now(clock).plusHours(1));
        given().port(port).auth().oauth2(alice).post("/bookings/" + boundary + "/cancel").then().statusCode(200);

        long tooLate = create(alice, LocalDateTime.now(clock).plusHours(1));
        Instant justAfterBoundary = clock.instant().plusSeconds(1);
        doReturn(justAfterBoundary).when(clock).instant();
        given().port(port).auth().oauth2(alice).get("/bookings/mine").then().statusCode(200)
                .body("content.find { it.id == " + tooLate + " }.cancellable", equalTo(false));
        given().port(port).auth().oauth2(alice).post("/bookings/" + tooLate + "/cancel").then().statusCode(409)
                .contentType("application/problem+json");
        assertEquals("REQUESTED", bookings.findById(tooLate).orElseThrow().getBookingStatus().name());
        given().port(port).auth().oauth2(staff).post("/bookings/" + tooLate + "/cancel").then().statusCode(200);
        assertEquals(2, bookings.findById(tooLate).orElseThrow().getStaffUpdateRevision());
    }

    @Test
    void bookingTimesUseQuarterHoursOpeningHoursAndTheBookingClock() {
        LocalDate day = LocalDate.now(clock);
        for (LocalTime time : List.of(LocalTime.of(15, 0), LocalTime.of(15, 15), LocalTime.of(20, 0))) {
            create(alice, day.atTime(time));
        }
        for (LocalTime time : List.of(LocalTime.of(14, 45), LocalTime.of(20, 15), LocalTime.of(17, 5),
                LocalTime.of(17, 15, 1), LocalTime.of(17, 15, 0, 1))) {
            given().port(port).auth().oauth2(alice).contentType("application/json")
                    .body(Map.of("restaurantId", restaurant.getId(), "customerName", "Guest", "customerEmail", "guest@example.com",
                            "time", day.atTime(time).toString(), "guests", 2))
                    .post("/bookings").then().statusCode(400).contentType("application/problem+json");
        }
        java.util.TimeZone previous = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
            // 16:30 in Stockholm, although the server's wall clock says 14:30.
            doReturn(Instant.parse("2099-06-01T14:30:00Z")).when(clock).instant();
            given().port(port).auth().oauth2(alice).contentType("application/json")
                    .body(Map.of("restaurantId", restaurant.getId(), "customerName", "Guest", "customerEmail", "guest@example.com",
                            "time", day.atTime(16, 15).toString(), "guests", 2))
                    .post("/bookings").then().statusCode(400);
            create(alice, day.atTime(16, 45));
        } finally {
            java.util.TimeZone.setDefault(previous);
        }
    }

    @Test
    void legacyBookingsWithoutAnOwnerRemainStaffOnly() {
        Booking legacy = bookings.save(new Booking(restaurant, "Legacy Guest", "same@example.com",
                LocalDateTime.now(clock).plusHours(2), 2));
        given().port(port).auth().oauth2(alice).get("/bookings/mine").then().statusCode(200)
                .body("totalElements", equalTo(0));
        given().port(port).auth().oauth2(alice).post("/bookings/" + legacy.getId() + "/cancel").then().statusCode(404);
        given().port(port).auth().oauth2(staff).get("/bookings/" + legacy.getId()).then().statusCode(200);
    }

    @Test
    void personalBookingsRequireAuthenticationAndReadScope() {
        given().port(port).get("/bookings/mine").then().statusCode(401);
        String noRead = token("auth0|alice", "GUEST", "bookings:write");
        given().port(port).auth().oauth2(noRead).get("/bookings/mine").then().statusCode(403);
    }

    private long create(String token, LocalDateTime time) {
        return given().port(port).auth().oauth2(token).contentType("application/json")
                .body(Map.of("restaurantId", restaurant.getId(), "customerName", "Guest", "customerEmail", "same@example.com",
                        "time", time.toString(), "guests", 2))
                .post("/bookings").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    private String token(String subject, String role, String scopes) {
        var claims = JwtClaimsSet.builder().issuer(TestJwtConfiguration.ISSUER).subject(subject)
                .audience(List.of(TestJwtConfiguration.AUDIENCE)).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300)).claim("scope", scopes)
                .claim(TestJwtConfiguration.ROLES_CLAIM, List.of(role)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId("test-key").build(), claims)).getTokenValue();
    }
}
