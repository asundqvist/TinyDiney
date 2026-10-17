package se.edugrade.java25.restaurant.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingSlotValidator implements ConstraintValidator<BookingSlot, LocalDateTime> {
    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotNull reports a missing time.
        LocalTime time = value.toLocalTime();
        return !time.isBefore(LocalTime.of(15, 0)) && !time.isAfter(LocalTime.of(20, 0))
                && time.getMinute() % 15 == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }
}
