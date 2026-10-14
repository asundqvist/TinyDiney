package se.edugrade.java25.restaurant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import se.edugrade.java25.restaurant.exceptions.InvalidBookingStateException;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String guestName;

    @Column(nullable = false)
    private String guestEmail;

    @Column(nullable = false)
    private LocalDateTime bookingTime;

    @Column(nullable = false)
    private int guestSize;

    @Column(nullable = false)
    private int revision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus bookingStatus = BookingStatus.REQUESTED;

    public Booking(Restaurant restaurant, String guestName, String guestEmail, LocalDateTime bookingTime, int guestSize) {
        this.restaurant = restaurant;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.bookingTime = bookingTime;
        this.guestSize = guestSize;
        this.revision = 1;
    }

    public boolean canConfirm() {
        return bookingStatus == BookingStatus.REQUESTED;
    }

    public boolean canSeat() {
        return bookingStatus == BookingStatus.CONFIRMED;
    }

    public boolean canCancel() {
        return bookingStatus == BookingStatus.REQUESTED || bookingStatus == BookingStatus.CONFIRMED;
    }

    public void confirm() {
        requireLegal(canConfirm(), "Booking with status '" + bookingStatus + "' cannot be confirmed.");
        transitionTo(BookingStatus.CONFIRMED);
    }

    public void seat() {
        requireLegal(canSeat(), "Booking with status '" + bookingStatus + "' cannot be seated.");
        transitionTo(BookingStatus.SEATED);
    }

    public void cancel() {
        requireLegal(canCancel(), "Booking with status '" + bookingStatus + "' cannot be cancelled.");
        transitionTo(BookingStatus.CANCELLED);
    }

    private void transitionTo(BookingStatus target) {
        this.bookingStatus = target;
        this.revision++;
    }

    private void requireLegal(boolean legal, String message) {
        if (!legal) {
            throw new InvalidBookingStateException(message);
        }
    }
}

