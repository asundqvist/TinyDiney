package se.edugrade.java25.restaurant.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import se.edugrade.java25.restaurant.controller.BookingController;
import se.edugrade.java25.restaurant.exceptions.GlobalExceptionHandler;
import se.edugrade.java25.restaurant.model.*;
import se.edugrade.java25.restaurant.repositories.*;
import se.edugrade.java25.restaurant.security.TestJwtConfiguration;
import se.edugrade.java25.restaurant.service.BookingService;
import se.edugrade.java25.restaurant.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
class BookingConcurrencyIntegrationTests {
    @Autowired BookingRepository bookings;
    @Autowired RestaurantRepository restaurants;
    @Autowired BookingService service;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean NotificationService notifications;

    @BeforeEach
    void clearDatabase() {
        bookings.deleteAll();
        restaurants.deleteAll();
    }

    @Test
    void simultaneousConfirmationAndCancellationOnlyCommitOneTransition() throws Exception {
        var restaurant = restaurants.save(new Restaurant("Family Kitchen", "Stockholm", "Family food", 40));
        var booking = bookings.saveAndFlush(new Booking(restaurant, "Guest", "guest@example.com",
                LocalDateTime.now().plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0), 2));
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var confirm = executor.submit(() -> change(booking.getId(), barrier, () -> service.confirm(booking.getId())));
            var cancel = executor.submit(() -> change(booking.getId(), barrier, () -> service.cancel(booking.getId())));
            Throwable first = confirm.get(15, TimeUnit.SECONDS);
            Throwable second = cancel.get(15, TimeUnit.SECONDS);
            assertTrue((first == null) != (second == null), "Exactly one request must succeed");
            assertInstanceOf(ObjectOptimisticLockingFailureException.class, first == null ? second : first);
        }
        var stored = bookings.findById(booking.getId()).orElseThrow();
        assertEquals(2, stored.getRevision());
        assertEquals(1, stored.getVersion());
        assertTrue(stored.getBookingStatus() == BookingStatus.CONFIRMED || stored.getBookingStatus() == BookingStatus.CANCELLED);
        verify(notifications, times(stored.getBookingStatus() == BookingStatus.CONFIRMED ? 1 : 0))
                .sendBookingConfirmation(any());
    }

    private Throwable change(Long id, CyclicBarrier barrier, Runnable action) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("staff", "", List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));
        SecurityContextHolder.setContext(context);
        try {
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                bookings.findById(id).orElseThrow();
                try { barrier.await(5, TimeUnit.SECONDS); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
                action.run();
            });
            return null;
        } catch (RuntimeException ex) {
            return ex;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void optimisticConflictReturns409ProblemDetail() throws Exception {
        var conflictingService = mock(BookingService.class);
        when(conflictingService.confirm(7L)).thenThrow(new ObjectOptimisticLockingFailureException(Booking.class, 7L));
        MockMvcBuilders.standaloneSetup(new BookingController(conflictingService))
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(post("/bookings/7/confirm"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("status").value(409))
                .andExpect(jsonPath("detail").value("This booking was changed by another request. Refresh the booking and try again."));
    }
}
