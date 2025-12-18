package com.screenleads.backend.app.application.security.jwt;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private SecretKey testKey;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        
        // Generate a valid HS256 key (at least 256 bits / 32 bytes)
        String base64Secret = Base64.getEncoder().encodeToString(
            "MyVerySecretKeyThatIsLongEnoughForHS256Algorithm".getBytes(StandardCharsets.UTF_8)
        );
        
        ReflectionTestUtils.setField(jwtService, "secretKey", base64Secret);
        jwtService.init();
        
        testKey = jwtService.getSigningKey();
        
        // Create test user
        Role role = new Role();
        role.setRole("ROLE_ADMIN");
        
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setRole(role);
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        // When
        String token = jwtService.generateToken(testUser);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length); // JWT has 3 parts
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        String username = jwtService.extractUsername(token);
        
        // Then
        assertEquals("testuser", username);
    }

    @Test
    void extractExpiration_ShouldReturnFutureDate() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        Date expiration = jwtService.extractExpiration(token);
        
        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void isTokenValid_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        boolean isValid = jwtService.isTokenValid(token, "testuser");
        
        // Then
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_WithWrongUsername_ShouldReturnFalse() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        boolean isValid = jwtService.isTokenValid(token, "wronguser");
        
        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
        // Given - Create an expired token
        String expiredToken = Jwts.builder()
                .subject(testUser.getUsername())
                .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 25)) // 25h ago
                .expiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1h ago (expired)
                .signWith(testKey, Jwts.SIG.HS256)
                .compact();
        
        // When
        boolean isValid = jwtService.isTokenValid(expiredToken, "testuser");
        
        // Then
        assertFalse(isValid);
    }

    @Test
    void extractClaim_ShouldExtractCorrectClaim() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        
        // Then
        assertEquals("testuser", subject);
    }

    @Test
    void generateToken_ShouldIncludeRoles() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        Claims claims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        List<?> roles = claims.get("roles", List.class);
        
        // Then
        assertNotNull(roles);
        assertFalse(roles.isEmpty());
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void generateToken_ShouldSetExpirationTo24Hours() {
        // Given
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken(testUser);
        
        // When
        Date expiration = jwtService.extractExpiration(token);
        long expectedExpiration = before + (1000L * 60 * 60 * 24);
        long actualExpiration = expiration.getTime();
        
        // Then - Allow 1 second tolerance for test execution time
        assertTrue(Math.abs(actualExpiration - expectedExpiration) < 1000);
    }

    @Test
    void init_ShouldSetupSigningKey() {
        // Given
        JwtService newService = new JwtService();
        String base64Secret = Base64.getEncoder().encodeToString(
            "AnotherSecretKeyThatIsLongEnoughForHS256".getBytes(StandardCharsets.UTF_8)
        );
        ReflectionTestUtils.setField(newService, "secretKey", base64Secret);
        
        // When
        newService.init();
        SecretKey key = newService.getSigningKey();
        
        // Then
        assertNotNull(key);
        assertEquals("HmacSHA256", key.getAlgorithm());
    }
}
