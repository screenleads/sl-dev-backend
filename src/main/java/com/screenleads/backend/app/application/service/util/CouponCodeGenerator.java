package com.screenleads.backend.app.application.service.util;

import java.security.SecureRandom;

public final class CouponCodeGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sin 0/O/I/1 para evitar confusión
    private static final SecureRandom RAND = new SecureRandom();

    private CouponCodeGenerator() {}

    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RAND.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
