package com.screenleads.backend.app.application.service.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CouponCodeGeneratorTest {

    @Test
    @DisplayName("Debería generar código con la longitud especificada")
    void generate_ShouldReturnCodeWithSpecifiedLength() {
        // Act
        String code = CouponCodeGenerator.generate(8);

        // Assert
        assertNotNull(code);
        assertEquals(8, code.length());
    }

    @Test
    @DisplayName("Debería generar código solo con caracteres permitidos")
    void generate_ShouldContainOnlyAllowedCharacters() {
        // Arrange
        String allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        // Act
        String code = CouponCodeGenerator.generate(100);

        // Assert
        for (char c : code.toCharArray()) {
            assertTrue(allowedChars.indexOf(c) >= 0, 
                    "Character '" + c + "' is not allowed");
        }
    }

    @Test
    @DisplayName("Debería excluir caracteres confusos (0, O, I, 1)")
    void generate_ShouldNotContainConfusingCharacters() {
        // Act
        String code = CouponCodeGenerator.generate(100);

        // Assert
        assertFalse(code.contains("0"), "Should not contain '0'");
        assertFalse(code.contains("O"), "Should not contain 'O'");
        assertFalse(code.contains("I"), "Should not contain 'I'");
        assertFalse(code.contains("1"), "Should not contain '1'");
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 6, 8, 10, 12, 16})
    @DisplayName("Debería generar códigos de diferentes longitudes")
    void generate_WithDifferentLengths_ShouldReturnCorrectLength(int length) {
        // Act
        String code = CouponCodeGenerator.generate(length);

        // Assert
        assertEquals(length, code.length());
    }

    @Test
    @DisplayName("Debería generar códigos únicos en llamadas sucesivas")
    void generate_SuccessiveCalls_ShouldReturnDifferentCodes() {
        // Act
        String code1 = CouponCodeGenerator.generate(12);
        String code2 = CouponCodeGenerator.generate(12);
        String code3 = CouponCodeGenerator.generate(12);

        // Assert
        assertNotEquals(code1, code2);
        assertNotEquals(code2, code3);
        assertNotEquals(code1, code3);
    }

    @Test
    @DisplayName("Debería generar código vacío para longitud 0")
    void generate_WithZeroLength_ShouldReturnEmptyString() {
        // Act
        String code = CouponCodeGenerator.generate(0);

        // Assert
        assertNotNull(code);
        assertEquals(0, code.length());
    }

    @Test
    @DisplayName("Debería generar códigos en mayúsculas")
    void generate_ShouldReturnUppercaseCode() {
        // Act
        String code = CouponCodeGenerator.generate(20);

        // Assert
        assertEquals(code, code.toUpperCase());
    }
}
