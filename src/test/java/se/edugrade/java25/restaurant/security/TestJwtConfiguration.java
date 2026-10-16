package se.edugrade.java25.restaurant.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

/** Local keys only for tests: production continues to use Auth0's public keys. */
@TestConfiguration(proxyBeanMethods = false)
public class TestJwtConfiguration {
    public static final String ISSUER = "https://test-issuer.example/";
    public static final String AUDIENCE = "https://restaurant-api";
    public static final String ROLES_CLAIM = "https://restaurant-api/roles";
    public static final String ALL_SCOPES =
            "restaurants:read restaurants:write bookings:read bookings:write bookings:manage";

    @Bean
    RSAKey testSigningKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate()).keyID("test-key").build();
    }

    @Bean
    JwtEncoder testJwtEncoder(RSAKey testSigningKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(testSigningKey)));
    }

    @Bean
    JwtDecoder testJwtDecoder(RSAKey testSigningKey,
                             @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
                             @Value("${spring.security.oauth2.resourceserver.jwt.audiences}") String audience) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(testSigningKey.toRSAPublicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>("aud", audiences -> audiences != null && audiences.contains(audience))));
        return decoder;
    }

    public static String token(JwtEncoder encoder, String role) {
        return token(encoder, role, ISSUER, AUDIENCE, ALL_SCOPES, Instant.now().plusSeconds(300));
    }

    public static String token(JwtEncoder encoder, String role, String issuer,
                               String audience, String scopes, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject("test-user")
                .audience(List.of(audience)).issuedAt(expiresAt.minusSeconds(300)).expiresAt(expiresAt)
                .claim("scope", scopes).claim(ROLES_CLAIM, List.of(role)).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build(), claims)).getTokenValue();
    }
}
