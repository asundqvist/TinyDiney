package se.edugrade.java25.restaurant.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;

class DeploymentConfigurationTests {
    @Test
    void mysqlAcceptsExternalDatabaseUrlAndProductionUsesExplicitSettings() {
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=mysql", "DB_URL=jdbc:mysql://remote-db:3306/tinydiney?sslMode=VERIFY_IDENTITY")
                .run(context -> assertEquals("jdbc:mysql://remote-db:3306/tinydiney?sslMode=VERIFY_IDENTITY",
                        context.getEnvironment().getProperty("spring.datasource.url")));
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod", "DB_URL=jdbc:mysql://production-db:3306/tinydiney?sslMode=REQUIRED",
                        "DB_USERNAME=app", "DB_PASSWORD=test-only", "AUTH0_ISSUER_URI=https://test-issuer.example/",
                        "AUTH0_AUDIENCE=https://restaurant-api", "FRONTEND_ORIGIN=https://tinydiney.example", "FORWARD_HEADERS_STRATEGY=framework")
                .run(context -> {
                    var environment = context.getEnvironment();
                    assertEquals("jdbc:mysql://production-db:3306/tinydiney?sslMode=REQUIRED", environment.getProperty("spring.datasource.url"));
                    assertEquals("https://tinydiney.example", environment.getProperty("app.cors.allowed-origin"));
                    assertEquals("https://test-issuer.example/", environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"));
                    assertEquals("framework", environment.getProperty("server.forward-headers-strategy"));
                });
    }
}
