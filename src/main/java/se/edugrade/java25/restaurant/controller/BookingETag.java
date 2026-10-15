package se.edugrade.java25.restaurant.controller;

public final class BookingETag {

    private BookingETag() {
    }

    public static String of(Long id, int revision) {
        return "\"booking-" + id + "-r" + revision + "\"";
    }
}
