package se.edugrade.java25.restaurant.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.edugrade.java25.restaurant.dto.*;
import se.edugrade.java25.restaurant.model.BookingStatus;
import se.edugrade.java25.restaurant.service.BookingService;
import se.edugrade.java25.restaurant.service.RestaurantService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@DisplayName("Behörigheter för samtliga roller och endpoints")
class AuthorizationIntegrationTests {
    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private RestaurantService restaurantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        RestaurantResponse restaurant = new RestaurantResponse(1L, "Bella", "Stockholm", "Italian", 40, null);
        BookingResponse booking = new BookingResponse(1L, 1L, "Guest", "guest@example.com",
                LocalDateTime.now().plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0), 2, BookingStatus.REQUESTED, "TD-ABCDEFGH-23456789", false);
        when(restaurantService.findAll(any())).thenReturn(new RestaurantPageResponse(List.of(restaurant), 0, 20, 1, 1));
        when(restaurantService.findById(1L)).thenReturn(restaurant);
        when(restaurantService.create(any())).thenReturn(restaurant);
        when(restaurantService.update(eq(1L), any())).thenReturn(restaurant);
        when(bookingService.findMine(any())).thenReturn(new MyBookingPageResponse(List.of(), 0, 20, 0, 0));
        when(bookingService.findAll(any())).thenReturn(new BookingPageResponse(List.of(booking), 0, 20, 1, 1));
        when(bookingService.findByIdWithRevision(1L)).thenReturn(new BookingResult(booking, 1));
        when(bookingService.create(eq(1L), any())).thenReturn(booking);
        when(bookingService.confirm(1L)).thenReturn(booking);
        when(bookingService.seat(1L)).thenReturn(booking);
        when(bookingService.cancel(1L)).thenReturn(booking);
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("roleMatrix")
    @DisplayName("Varje roll får rätt åtkomst till varje endpoint")
    void roleMatrix(String role, Endpoint endpoint) throws Exception {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        for (String scope : TestJwtConfiguration.ALL_SCOPES.split(" ")) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
        mockMvc.perform(requestFor(endpoint).with(jwt().authorities(authorities)))
                .andExpect(status().is(endpoint.roles().contains(role) ? endpoint.successStatus() : 403));
    }

    @Test
    @DisplayName("A protected endpoint without a token returns 401")
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(get("/bookings")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin without the required write scope returns 403")
    void missingScopeReturns403() throws Exception {
        Endpoint endpoint = new Endpoint("POST", "/restaurants", 201, Set.of("ADMIN"));
        mockMvc.perform(requestFor(endpoint).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scopes utan roll ger 403")
    void scopeWithoutRoleReturns403() throws Exception {
        mockMvc.perform(get("/bookings").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_bookings:read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tillåten frontend kan göra CORS-preflight utan token")
    void allowedCorsOrigin() throws Exception {
        mockMvc.perform(options("/bookings").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("Okänd frontend nekas CORS-preflight")
    void unknownCorsOrigin() throws Exception {
        mockMvc.perform(options("/bookings").header("Origin", "https://unknown.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Health och Swagger är publika medan odokumenterade endpoints nekas")
    void publicEndpointsAndDefaultDeny() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mockMvc.perform(delete("/restaurants/1").with(jwt())).andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder requestFor(Endpoint endpoint) {
        MockHttpServletRequestBuilder request = request(endpoint.method(), URI.create(endpoint.path()));
        if (!endpoint.method().equals("GET")) {
            String body = endpoint.path().startsWith("/restaurants")
                    ? """
                      {"name":"Bella","city":"Stockholm","cuisine":"Italian","seats":40}
                      """
                    : """
                      {"restaurantId":1,"customerName":"Guest","customerEmail":"guest@example.com",
                       "time":"%s","guests":2}
                      """.formatted(LocalDateTime.now().plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0));
            request.contentType("application/json").content(body);
        }
        return request;
    }

    static Stream<Arguments> roleMatrix() {
        return Stream.of("GUEST", "STAFF", "ADMIN").flatMap(role -> endpoints().map(endpoint -> Arguments.of(role, endpoint)));
    }

    static Stream<Endpoint> endpoints() {
        Set<String> everyone = Set.of("GUEST", "STAFF", "ADMIN");
        return Stream.of(
                new Endpoint("GET", "/restaurants", 200, everyone),
                new Endpoint("GET", "/restaurants/1", 200, everyone),
                new Endpoint("POST", "/restaurants", 201, Set.of("ADMIN")),
                new Endpoint("PUT", "/restaurants/1", 200, Set.of("ADMIN")),
                new Endpoint("GET", "/bookings", 200, everyone),
                new Endpoint("GET", "/bookings/1", 200, everyone),
                new Endpoint("GET", "/bookings/mine", 200, everyone),
                new Endpoint("POST", "/bookings", 201, everyone),
                new Endpoint("POST", "/bookings/1/confirm", 200, Set.of("STAFF", "ADMIN")),
                new Endpoint("POST", "/bookings/1/seat", 200, Set.of("STAFF", "ADMIN")),
                new Endpoint("POST", "/bookings/1/cancel", 200, everyone));
    }

    record Endpoint(String method, String path, int successStatus, Set<String> roles) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }
}
