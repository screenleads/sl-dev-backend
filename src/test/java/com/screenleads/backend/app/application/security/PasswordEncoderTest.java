package com.screenleads.backend.app.application.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordEncoder Tests")
class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("Debería encriptar contraseñas correctamente")
    void encode_ShouldHashPassword() {
        // Arrange
        String rawPassword = "mySecretPassword123";

        // Act
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"));
        assertTrue(encodedPassword.length() >= 60);
    }

    @Test
    @DisplayName("Debería validar contraseñas correctas")
    void matches_WithCorrectPassword_ShouldReturnTrue() {
        // Arrange
        String rawPassword = "testPassword456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Assert
        assertTrue(matches);
    }

    @Test
    @DisplayName("Debería rechazar contraseñas incorrectas")
    void matches_WithWrongPassword_ShouldReturnFalse() {
        // Arrange
        String rawPassword = "correctPassword";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        // Assert
        assertFalse(matches);
    }

    @Test
    @DisplayName("Debería generar hashes diferentes para la misma contraseña")
    void encode_SamePassword_ShouldGenerateDifferentHashes() {
        // Arrange
        String password = "samePassword";

        // Act
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        // Assert
        assertNotEquals(hash1, hash2, "Los hashes deberían ser diferentes (salts únicos)");
        assertTrue(passwordEncoder.matches(password, hash1));
        assertTrue(passwordEncoder.matches(password, hash2));
    }

    @Test
    @DisplayName("Debería manejar contraseñas vacías")
    void encode_EmptyPassword_ShouldWork() {
        // Arrange
        String emptyPassword = "";

        // Act
        String encodedPassword = passwordEncoder.encode(emptyPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(emptyPassword, encodedPassword));
    }

    @Test
    @DisplayName("Debería manejar contraseñas con caracteres especiales")
    void encode_PasswordWithSpecialCharacters_ShouldWork() {
        // Arrange
        String complexPassword = "P@ssw0rd!#$%&*()";

        // Act
        String encodedPassword = passwordEncoder.encode(complexPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(complexPassword, encodedPassword));
    }

    @Test
    @DisplayName("Debería codificar contraseñas largas (dentro del límite de 72 bytes de BCrypt)")
    void encode_LongPassword_ShouldWork() {
        // Arrange
        String longPassword = "a".repeat(70); // BCrypt limits passwords to 72 bytes

        // Act
        String encodedPassword = passwordEncoder.encode(longPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(longPassword, encodedPassword));
    }

    @Test
    @DisplayName("No debería validar null contra hash válido")
    void matches_WithNullPassword_ShouldReturnFalse() {
        // Arrange
        String encodedPassword = passwordEncoder.encode("somePassword");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            passwordEncoder.matches(null, encodedPassword)
        );
    }
}
