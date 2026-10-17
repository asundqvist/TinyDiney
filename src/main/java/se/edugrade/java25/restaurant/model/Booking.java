package se.edugrade.java25.restaurant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import se.edugrade.java25.restaurant.exceptions.InvalidBookingStateException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private static final SecureRandom REFERENCE_RANDOM = new SecureRandom();
    private static final String REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    // Nullable only to allow Hibernate to add the column to existing databases before backfill.
    @Column(name = "booking_reference", length = 20, unique = true)
    private String reference;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String guestName;

    @Column(nullable = false)
    private String guestEmail;

    // Nullable for bookings created before account ownership was introduced.
    @Column(name = "owner_subject", length = 255)
    private String ownerSubject;

    @Column(nullable = false)
    private LocalDateTime bookingTime;

    @Column(nullable = false)
    private int guestSize;

    @Column(nullable = false)
    private int revision;

    // Separate from the normal revision so guests' own cancellations do not create staff notifications.
    private Integer staffUpdateRevision;

    // Nullable for older bookings; only new guest cancellations of confirmed tables set this flag.
    private Boolean guestCancelledAfterConfirmation;

    public boolean wasGuestCancelledAfterConfirmation() {
        return Boolean.TRUE.equals(guestCancelledAfterConfirmation);
    }

    public void cancelByGuest() {
        boolean wasConfirmed = bookingStatus == BookingStatus.CONFIRMED;
        cancel();
        guestCancelledAfterConfirmation = wasConfirmed;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus bookingStatus = BookingStatus.REQUESTED;

    public Booking(Restaurant restaurant, String guestName, String guestEmail, LocalDateTime bookingTime, int guestSize) {
        this.restaurant = restaurant;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.bookingTime = bookingTime;
        this.guestSize = guestSize;
        ensureReference();
        this.revision = 1;
    }

    public Booking(Restaurant restaurant, String guestName, String guestEmail, LocalDateTime bookingTime,
                   int guestSize, String ownerSubject) {
        this(restaurant, guestName, guestEmail, bookingTime, guestSize);
        this.ownerSubject = ownerSubject;
    }

    @PrePersist
    public void ensureReference() {
        if (reference != null) return;
        var value = new StringBuilder("TD-");
        for (int index = 0; index < 16; index++) {
            if (index == 8) value.append('-');
            value.append(REFERENCE_ALPHABET.charAt(REFERENCE_RANDOM.nextInt(REFERENCE_ALPHABET.length())));
        }
        reference = value.toString();
        // Adding a reference to an old booking also changes its HTTP representation.
        revision++;
    }

    public OffsetDateTime guestCancellationDeadline(ZoneId zone) {
        return bookingTime.atZone(zone).minusHours(1).toOffsetDateTime();
    }

    public boolean canGuestCancel(Clock clock) {
        return canCancel() && !clock.instant().isAfter(guestCancellationDeadline(clock.getZone()).toInstant());
    }

    public void markHandledByStaff() {
        staffUpdateRevision = revision;
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
