package com.screenleads.backend.app.application.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidad para generar y hashear API Keys de forma segura
 */
@Component
public class ApiKeyGenerator {
    
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String LIVE_PREFIX = "sk_live_";
    private static final String TEST_PREFIX = "sk_test_";
    private static final int KEY_LENGTH = 32; // bytes

    /**
     * Genera una nueva API key completa con prefijo
     * @param isLive true para ambiente live, false para test
     * @return API key completa: sk_live_xxxxx... o sk_test_xxxxx...
     */
    public String generateApiKey(boolean isLive) {
        String prefix = isLive ? LIVE_PREFIX : TEST_PREFIX;
        String randomPart = generateRandomString(KEY_LENGTH);
        return prefix + randomPart;
    }

    /**
     * Genera una API key live por defecto
     */
    public String generateApiKey() {
        return generateApiKey(true);
    }

    /**
     * Hashea una API key usando BCrypt
     * @param apiKey La key en texto plano
     * @return Hash BCrypt de la key
     */
    public String hashApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        return encoder.encode(apiKey);
    }

    /**
     * Verifica si una API key coincide con un hash
     * @param rawApiKey API key en texto plano
     * @param hashedApiKey Hash almacenado en BD
     * @return true si coincide, false si no
     */
    public boolean matchesHash(String rawApiKey, String hashedApiKey) {
        if (rawApiKey == null || hashedApiKey == null) {
            return false;
        }
        return encoder.matches(rawApiKey, hashedApiKey);
    }

    /**
     * Extrae el prefijo visible de una API key (primeros 12 caracteres)
     * Ejemplo: "sk_live_abc1"
     * @param apiKey API key completa
     * @return Prefijo de 12 caracteres
     */
    public String extractPrefix(String apiKey) {
        if (apiKey == null || apiKey.length() < 12) {
            throw new IllegalArgumentException("Invalid API key format");
        }
        return apiKey.substring(0, 12);
    }

    /**
     * Genera un string aleatorio seguro
     */
    private String generateRandomString(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        secureRandom.nextBytes(randomBytes);
        
        // Codificar en base64 URL-safe y eliminar caracteres problemáticos
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes)
                .replaceAll("[^a-zA-Z0-9]", "")
                .substring(0, Math.min(40, byteLength * 2)); // Limitar longitud
    }

    /**
     * Valida el formato de una API key
     * @param apiKey Key a validar
     * @return true si el formato es válido
     */
    public boolean isValidFormat(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        
        // Debe empezar con sk_live_ o sk_test_
        if (!apiKey.startsWith(LIVE_PREFIX) && !apiKey.startsWith(TEST_PREFIX)) {
            return false;
        }
        
        // Longitud mínima razonable
        return apiKey.length() >= 20;
    }

    /**
     * Determina si una API key es de ambiente live o test
     * @param apiKey Key a verificar
     * @return true si es live, false si es test
     */
    public boolean isLiveKey(String apiKey) {
        return apiKey != null && apiKey.startsWith(LIVE_PREFIX);
    }
}
