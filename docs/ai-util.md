# Utils & Helpers — snapshot incrustado

> Clases utilitarias.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/application/service/util/CouponCodeGenerator.java
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

```

```java
// src/main/java/com/screenleads/backend/app/application/service/util/IdentifierNormalizer.java
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

```

