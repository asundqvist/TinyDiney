package se.edugrade.java25.restaurant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BookingClockConfig {
    @Bean
    LocalValidatorFactoryBean bookingValidator(Clock clock) {
        var validator = new LocalValidatorFactoryBean();
        validator.setConfigurationInitializer(configuration -> configuration.clockProvider(() -> clock));
        return validator;
    }

    @Bean
    Clock bookingClock(@Value("${app.booking-zone:Europe/Stockholm}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
