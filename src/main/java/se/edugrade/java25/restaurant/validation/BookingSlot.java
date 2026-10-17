package se.edugrade.java25.restaurant.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = BookingSlotValidator.class)
public @interface BookingSlot {
    String message() default "Choose a quarter-hour time between 15:00 and 20:00.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
