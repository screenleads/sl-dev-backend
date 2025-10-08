package com.screenleads.backend.app.application.service.util;

import com.screenleads.backend.app.domain.model.LeadIdentifierType;

public final class IdentifierNormalizer {

    private IdentifierNormalizer() {}

    public static String normalize(LeadIdentifierType type, String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        switch (type) {
            case EMAIL:
                return s.toLowerCase();
            case PHONE:
                // Normalización simple: quitar espacios/guiones, convertir 00xx a +xx
                String p = s.replaceAll("[^+\\d]", "");
                if (p.startsWith("00")) {
                    p = "+" + p.substring(2);
                }
                return p;
            case DOCUMENT:
                return s.replaceAll("\\s+", "").toUpperCase();
            case OTHER:
            default:
                return s;
        }
    }
}
