package com.screenleads.backend.app.application.security.jwt;

import static org.assertj.core.api.Assertions.*;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Comprehensive test suite for JwtService.
 * 
 * This test class demonstrates:
 * - Testing security-critical components
 * - Token generation and validation
 * - Edge case handling (expired tokens, malformed tokens, null inputs)
 * - Using real implementation instead of mocks for security components
 * - AssertJ fluent assertions for clarity
 * 
 * Pattern: Real JwtService with test secret key for reproducible tests
 */
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private String testSecretKey;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        // Use a known test secret key (Base64 encoded, must be at least 256 bits for
        // HS256)
        testSecretKey = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3Qtc2VydmljZS10ZXN0LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZw==";
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecretKey));

        // Create JwtService with reflection to set the secret key
        jwtService = new JwtService();
        setSecretKey(jwtService, testSecretKey);
        jwtService.init(); // Call @PostConstruct manually
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT token with username and roles")
        void shouldGenerateValidTokenWithUsernameAndRoles() {
            // Given: A user with a role (User model has single Role, not multiple)
            User user = createTestUser("testuser@example.com", "ROLE_USER");

            // When: Generating a token
            String token = jwtService.generateToken(user);

            // Then: Token should not be null or empty
            assertThat(token).isNotNull().isNotEmpty();

            // And: Token should be parsable with correct claims
            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo("testuser@example.com");
            assertThat(claims.get("roles")).asList()
                    .containsExactlyInAnyOrder("ROLE_USER");
        }

        @Test
        @DisplayName("Should generate token with 24-hour expiration")
        void shouldGenerateTokenWith24HourExpiration() {
            // Given
            User user = createTestUser("testuser@example.com", "ROLE_USER");
            Date beforeGeneration = new Date();

            // When
            String token = jwtService.generateToken(user);

            // Then
            Date afterGeneration = new Date();
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();

            // Token should expire approximately 24 hours from now
            long expectedExpirationTime = beforeGeneration.getTime() + (1000 * 60 * 60 * 24);
            long actualExpirationTime = expiration.getTime();

            // Allow 1 second tolerance for test execution time
            assertThat(actualExpirationTime)
                    .isGreaterThanOrEqualTo(expectedExpirationTime - 1000)
                    .isLessThanOrEqualTo(afterGeneration.getTime() + (1000 * 60 * 60 * 24) + 1000);
        }

        @Test
        @DisplayName("Should handle user with no roles")
        void shouldHandleUserWithNoRoles() {
            // Given: User with no role (null role)
            User user = createTestUser("noroleuser@example.com", new String[] {});

            // When
            String token = jwtService.generateToken(user);

            // Then: Token is generated
            // IMPORTANT: User.getAuthorities() has a fallback to "ROLE_USER" when role is
            // null
            // This is the expected domain behavior (see User.java:66-68)
            assertThat(token).isNotNull();
            Claims claims = parseToken(token);
            Object rolesObj = claims.get("roles");
            assertThat(rolesObj)
                    .asList()
                    .containsExactly("ROLE_USER"); // Default fallback from User.getAuthorities()
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate token with matching username")
        void shouldValidateTokenWithMatchingUsername() {
            // Given
            User user = createTestUser("valid@example.com", "ROLE_USER");
            String token = jwtService.generateToken(user);

            // When
            boolean isValid = jwtService.isTokenValid(token, "valid@example.com");

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token with non-matching username")
        void shouldRejectTokenWithNonMatchingUsername() {
            // Given
            User user = createTestUser("user1@example.com", "ROLE_USER");
            String token = jwtService.generateToken(user);

            // When
            boolean isValid = jwtService.isTokenValid(token, "user2@example.com");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should validate token with UserDetails")
        void shouldValidateTokenWithUserDetails() {
            // Given
            User user = createTestUser("userdetails@example.com", "ROLE_USER");
            String token = jwtService.generateToken(user);

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("userdetails@example.com")
                    .password("password")
                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                    .build();

            // When
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Given: An expired token (created 25 hours ago)
            String expiredToken = createExpiredToken("expired@example.com");

            // When/Then: Should throw ExpiredJwtException when trying to validate
            assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, "expired@example.com"))
                    .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.jwt.token";

            // When/Then
            assertThatThrownBy(() -> jwtService.isTokenValid(malformedToken, "user@example.com"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            // When/Then
            assertThatThrownBy(() -> jwtService.isTokenValid(null, "user@example.com"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract username from valid token")
        void shouldExtractUsernameFromValidToken() {
            // Given
            User user = createTestUser("extract@example.com", "ROLE_USER");
            String token = jwtService.generateToken(user);

            // When
            String username = jwtService.extractUsername(token);

            // Then
            assertThat(username).isEqualTo("extract@example.com");
        }

        @Test
        @DisplayName("Should extract expiration from valid token")
        void shouldExtractExpirationFromValidToken() {
            // Given
            User user = createTestUser("expiry@example.com", "ROLE_USER");
            String token = jwtService.generateToken(user);

            // When
            Date expiration = jwtService.extractExpiration(token);

            // Then
            assertThat(expiration).isInTheFuture();
        }

        @Test
        @DisplayName("Should extract custom claim from token")
        void shouldExtractCustomClaimFromToken() {
            // Given: User with single role
            User user = createTestUser("claims@example.com", "ROLE_USER");
            String token = jwtService.generateToken(user);

            // When
            Object roles = jwtService.extractClaim(token, claims -> claims.get("roles"));

            // Then: Should contain only one role
            assertThat(roles).asList()
                    .containsExactlyInAnyOrder("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("HTTP Request Token Resolution Tests")
    class HttpRequestTokenResolutionTests {

        @Test
        @DisplayName("Should resolve token from Authorization header with Bearer prefix")
        void shouldResolveTokenFromAuthorizationHeader() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
            request.addHeader("Authorization", "Bearer " + token);

            // When
            String resolvedToken = jwtService.resolveToken(request);

            // Then
            assertThat(resolvedToken).isEqualTo(token);
        }

        @Test
        @DisplayName("Should return null when Authorization header is missing")
        void shouldReturnNullWhenAuthorizationHeaderMissing() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();

            // When
            String resolvedToken = jwtService.resolveToken(request);

            // Then
            assertThat(resolvedToken).isNull();
        }

        @Test
        @DisplayName("Should return null when Authorization header does not start with Bearer")
        void shouldReturnNullWhenAuthorizationHeaderNotBearer() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            // When
            String resolvedToken = jwtService.resolveToken(request);

            // Then
            assertThat(resolvedToken).isNull();
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void shouldHandleNullRequestGracefully() {
            // When/Then
            assertThatThrownBy(() -> jwtService.resolveToken(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a test user with specified username and roles
     */
    private User createTestUser(String username, String... roles) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username);
        user.setPassword("encoded_password");

        // Set role (authorities are derived from role)
        // Only set role if roles array is not empty
        if (roles != null && roles.length > 0 && roles[0] != null && !roles[0].isEmpty()) {
            Role role = new Role();
            role.setRole(roles[0]); // Use first role
            user.setRole(role);
        }

        return user;
    }

    /**
     * Parses a JWT token and returns the claims
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Creates an expired token for testing expiration validation
     */
    private String createExpiredToken(String username) {
        Date pastDate = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 25)); // 25 hours ago

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 49))) // 49 hours ago
                .expiration(pastDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Uses reflection to set the secret key in JwtService for testing
     */
    private void setSecretKey(JwtService jwtService, String secretKey) {
        try {
            java.lang.reflect.Field field = JwtService.class.getDeclaredField("secretKey");
            field.setAccessible(true);
            field.set(jwtService, secretKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set secret key via reflection", e);
        }
    }
}
