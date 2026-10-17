package se.edugrade.java25.restaurant.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.java25.restaurant.model.Restaurant;
import se.edugrade.java25.restaurant.repositories.RestaurantRepository;
import tools.jackson.databind.json.JsonMapper;

/** Seeds an empty database and migrates known legacy names without recreating admin-renamed demos. */
@Component
@Profile("(mysql | prod) & !test")
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoRestaurantInitializer implements ApplicationRunner {
    private final RestaurantRepository restaurants;

    @Value("classpath:demo/restaurants.json")
    private Resource catalogue;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        boolean seedEmptyDatabase = restaurants.count() == 0;
        try (var input = catalogue.getInputStream()) {
            var entries = JsonMapper.builder().build().readTree(input);
            for (var entry : entries) {
                String key = entry.get("demoKey").asString();
                if (restaurants.findByDemoKey(key).isPresent()) continue;
                String name = entry.get("name").asString();
                String city = entry.get("city").asString();
                Restaurant existing = restaurants.findByNameAndCity(name, city).orElse(null);
                var legacyNames = entry.get("legacyNames");
                if (existing == null && legacyNames != null && legacyNames.isArray()) {
                    for (var legacyName : legacyNames) {
                        existing = restaurants.findByNameAndCity(legacyName.asString(), city).orElse(null);
                        if (existing != null) break;
                    }
                    if (existing != null) existing.update(name, city, entry.get("cuisine").asString(), existing.getSeats());
                }
                if (existing == null) {
                    var version = entry.get("seedVersion");
                    // Version 2 expands existing databases; stable keys prevent duplicates after admin edits.
                    if (!seedEmptyDatabase && (version == null || version.asInt() != 2)) continue;
                    existing = new Restaurant(name, city, entry.get("cuisine").asString(), entry.get("seats").asInt());
                }
                existing.identifyDemo(key);
                restaurants.save(existing);
            }
        }
    }
}
