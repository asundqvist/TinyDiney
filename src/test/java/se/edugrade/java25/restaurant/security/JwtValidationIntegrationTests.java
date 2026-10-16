package se.edugrade.java25.restaurant.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@DisplayName("JWT-validering via riktiga HTTP-anrop")
class JwtValidationIntegrationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("En signerad token med Auth0-rollclaim och scope ger åtkomst")
    void signedTokenWithRoleAndScopeIsAccepted() {
        given().port(port).auth().oauth2(TestJwtConfiguration.token(jwtEncoder, "guest"))
                .get("/bookings").then().statusCode(200).header("Set-Cookie", nullValue());
    }

    @Test
    @DisplayName("Saknad eller trasig token ger 401 med Bearer-utmaning")
    void missingOrMalformedTokenIsRejected() {
        given().port(port).get("/bookings").then().statusCode(401).header("WWW-Authenticate", startsWith("Bearer"));
        given().port(port).auth().oauth2("not-a-jwt").get("/bookings").then().statusCode(401);
    }

    @Test
    @DisplayName("Fel issuer, fel audience och utgången token ger 401")
    void invalidClaimsAreRejected() {
        reject(TestJwtConfiguration.token(jwtEncoder, "ADMIN", "https://other-issuer.example/",
                TestJwtConfiguration.AUDIENCE, TestJwtConfiguration.ALL_SCOPES, Instant.now().plusSeconds(300)));
        reject(TestJwtConfiguration.token(jwtEncoder, "ADMIN", TestJwtConfiguration.ISSUER,
                "https://management-api", TestJwtConfiguration.ALL_SCOPES, Instant.now().plusSeconds(300)));
        reject(TestJwtConfiguration.token(jwtEncoder, "ADMIN", TestJwtConfiguration.ISSUER,
                TestJwtConfiguration.AUDIENCE, TestJwtConfiguration.ALL_SCOPES, Instant.now().minusSeconds(120)));
    }

    @Test
    @DisplayName("Ändrad signatur ger 401")
    void invalidSignatureIsRejected() {
        String token = TestJwtConfiguration.token(jwtEncoder, "ADMIN");
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        reject(token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1));
    }

    @Test
    @DisplayName("En giltig token med fel roll eller saknat scope ger 403")
    void insufficientPermissionsAreRejected() {
        given().port(port).auth().oauth2(TestJwtConfiguration.token(jwtEncoder, "GUEST"))
                .post("/bookings/1/confirm").then().statusCode(403);
        String token = TestJwtConfiguration.token(jwtEncoder, "ADMIN", TestJwtConfiguration.ISSUER,
                TestJwtConfiguration.AUDIENCE, "", Instant.now().plusSeconds(300));
        given().port(port).auth().oauth2(token).get("/bookings").then().statusCode(403);
    }

    private void reject(String token) {
        given().port(port).auth().oauth2(token).get("/bookings").then().statusCode(401);
    }
}
