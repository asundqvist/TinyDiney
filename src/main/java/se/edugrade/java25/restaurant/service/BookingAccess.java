package se.edugrade.java25.restaurant.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class BookingAccess {
    public String subject() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt && jwt.isAuthenticated()
                && jwt.getToken().getSubject() != null && !jwt.getToken().getSubject().isBlank()) {
            return jwt.getToken().getSubject();
        }
        throw new AccessDeniedException("A user identity is required for bookings");
    }

    public boolean isStaff() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF")
                        || authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
