package com.screenleads.backend.app.application.service.util;

import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierNormalizerTest {

    @Test
    @DisplayName("Debería normalizar email a minúsculas")
    void normalize_Email_ShouldConvertToLowercase() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.EMAIL, "Test@Example.COM");

        // Assert
        assertEquals("test@example.com", result);
    }

    @Test
    @DisplayName("Debería eliminar espacios en email")
    void normalize_Email_ShouldTrimSpaces() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.EMAIL, "  test@example.com  ");

        // Assert
        assertEquals("test@example.com", result);
    }

    @ParameterizedTest
    @CsvSource({
            "123 456 789, 123456789",
            "123-456-789, 123456789",
            "0034 123 456 789, +34123456789",
            "+34 123 456 789, +34123456789",
            "00 1 555-1234, +15551234"
    })
    @DisplayName("Debería normalizar números de teléfono")
    void normalize_Phone_ShouldFormatCorrectly(String input, String expected) {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.PHONE, input);

        // Assert
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Debería convertir 00 en + para teléfonos")
    void normalize_Phone_ShouldConvert00ToPlus() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.PHONE, "0034612345678");

        // Assert
        assertEquals("+34612345678", result);
    }

    @Test
    @DisplayName("Debería normalizar documentos a mayúsculas sin espacios")
    void normalize_Document_ShouldConvertToUppercaseAndRemoveSpaces() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.DOCUMENT, "abc 123 def");

        // Assert
        assertEquals("ABC123DEF", result);
    }

    @Test
    @DisplayName("Debería eliminar múltiples espacios en documentos")
    void normalize_Document_ShouldRemoveMultipleSpaces() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.DOCUMENT, "12345   678   X");

        // Assert
        assertEquals("12345678X", result);
    }

    @Test
    @DisplayName("Debería mantener OTHER sin cambios excepto trim")
    void normalize_Other_ShouldOnlyTrim() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.OTHER, "  Some Value  ");

        // Assert
        assertEquals("Some Value", result);
    }

    @Test
    @DisplayName("Debería retornar null cuando input es null")
    void normalize_WithNullInput_ShouldReturnNull() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.EMAIL, null);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Debería manejar string vacío")
    void normalize_WithEmptyString_ShouldReturnEmptyString() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.EMAIL, "");

        // Assert
        assertEquals("", result);
    }

    @Test
    @DisplayName("Debería manejar string solo con espacios")
    void normalize_WithOnlySpaces_ShouldReturnEmptyString() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.DOCUMENT, "   ");

        // Assert
        assertEquals("", result);
    }

    @Test
    @DisplayName("Debería preservar caracteres especiales en email")
    void normalize_Email_ShouldPreserveSpecialCharacters() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.EMAIL, "user+tag@example.com");

        // Assert
        assertEquals("user+tag@example.com", result);
    }

    @Test
    @DisplayName("Debería normalizar teléfono sin prefijo internacional")
    void normalize_Phone_WithoutInternationalPrefix() {
        // Act
        String result = IdentifierNormalizer.normalize(LeadIdentifierType.PHONE, "612 345 678");

        // Assert
        assertEquals("612345678", result);
    }
}
