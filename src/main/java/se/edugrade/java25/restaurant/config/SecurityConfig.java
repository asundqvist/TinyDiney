package se.edugrade.java25.restaurant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                           JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                // Bearer tokens are supplied explicitly in Authorization, never in cookies or sessions.
                // Browsers cannot attach authentication automatically, so this stateless API disables CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/restaurants", "/restaurants/*")
                        .access(permission("restaurants:read", "GUEST", "STAFF", "ADMIN"))
                        .requestMatchers(HttpMethod.POST, "/restaurants")
                        .access(permission("restaurants:write", "ADMIN"))
                        .requestMatchers(HttpMethod.PUT, "/restaurants/*")
                        .access(permission("restaurants:write", "ADMIN"))
                        .requestMatchers(HttpMethod.GET, "/bookings", "/bookings/*")
                        .access(permission("bookings:read", "GUEST", "STAFF", "ADMIN"))
                        .requestMatchers(HttpMethod.POST, "/bookings")
                        .access(permission("bookings:write", "GUEST", "STAFF", "ADMIN"))
                        .requestMatchers(HttpMethod.POST, "/bookings/*/confirm", "/bookings/*/seat")
                        .access(permission("bookings:manage", "STAFF", "ADMIN"))
                        .requestMatchers(HttpMethod.POST, "/bookings/*/cancel")
                        .access(permission("bookings:write", "GUEST", "STAFF", "ADMIN"))
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    private WebExpressionAuthorizationManager permission(String scope, String... roles) {
        return new WebExpressionAuthorizationManager(
                "hasAuthority('SCOPE_" + scope + "') and hasAnyRole('" + String.join("','", roles) + "')");
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(@Value("${auth0.roles-claim}") String rolesClaim) {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            Object roles = jwt.getClaim(rolesClaim);
            if (roles instanceof Collection<?> values) {
                values.stream().filter(String.class::isInstance).map(String.class::cast)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
                        .forEach(authorities::add);
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String origin) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(origin));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "If-None-Match"));
        cors.setExposedHeaders(List.of("Location", "ETag"));
        cors.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
