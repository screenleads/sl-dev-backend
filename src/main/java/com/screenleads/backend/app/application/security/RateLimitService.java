package com.screenleads.backend.app.application.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio de rate limiting para API keys
 * - Test keys: 100 req/min
 * - Live keys: 1000 req/min
 */
@Service
@Slf4j
public class RateLimitService {

    private static final int TEST_KEY_LIMIT_PER_MINUTE = 100;
    private static final int LIVE_KEY_LIMIT_PER_MINUTE = 1000;

    // Map: apiKeyId -> RequestWindow
    private final Map<Long, RequestWindow> requestWindows = new ConcurrentHashMap<>();

    /**
     * Verifica si la API key puede realizar una petición
     * 
     * @param apiKeyId ID de la API key
     * @param isLive   true si es live key, false si es test
     * @return true si puede hacer la petición, false si excede el límite
     */
    public boolean allowRequest(Long apiKeyId, boolean isLive) {
        int limit = isLive ? LIVE_KEY_LIMIT_PER_MINUTE : TEST_KEY_LIMIT_PER_MINUTE;

        RequestWindow window = requestWindows.computeIfAbsent(apiKeyId,
                k -> new RequestWindow(Instant.now()));

        return window.allowRequest(limit);
    }

    /**
     * Obtiene información del rate limit actual para una key
     */
    public RateLimitInfo getRateLimitInfo(Long apiKeyId, boolean isLive) {
        int limit = isLive ? LIVE_KEY_LIMIT_PER_MINUTE : TEST_KEY_LIMIT_PER_MINUTE;
        RequestWindow window = requestWindows.get(apiKeyId);

        if (window == null) {
            return new RateLimitInfo(limit, limit, 60);
        }

        int remaining = Math.max(0, limit - window.getCount());
        long resetIn = window.getSecondsUntilReset();

        return new RateLimitInfo(limit, remaining, resetIn);
    }

    /**
     * Limpia ventanas de peticiones antiguas cada 5 minutos
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    public void cleanupOldWindows() {
        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);

        requestWindows.entrySet().removeIf(entry -> entry.getValue().getWindowStart().isBefore(fiveMinutesAgo));

        log.debug("🧹 Limpieza de rate limit windows. Activas: {}", requestWindows.size());
    }

    /**
     * Ventana de tiempo para contar peticiones
     */
    private static class RequestWindow {
        private final Instant windowStart;
        private final AtomicInteger count;

        public RequestWindow(Instant start) {
            this.windowStart = start;
            this.count = new AtomicInteger(0);
        }

        public synchronized boolean allowRequest(int limit) {
            // Si la ventana expiró (más de 60 segundos), resetear
            if (Instant.now().isAfter(windowStart.plusSeconds(60))) {
                count.set(0);
                return true;
            }

            // Verificar si aún hay cuota disponible
            if (count.get() >= limit) {
                return false;
            }

            count.incrementAndGet();
            return true;
        }

        public int getCount() {
            // Si la ventana expiró, retornar 0
            if (Instant.now().isAfter(windowStart.plusSeconds(60))) {
                return 0;
            }
            return count.get();
        }

        public Instant getWindowStart() {
            return windowStart;
        }

        public long getSecondsUntilReset() {
            long secondsSinceStart = Instant.now().getEpochSecond() - windowStart.getEpochSecond();
            return Math.max(0, 60 - secondsSinceStart);
        }
    }

    /**
     * Información del estado del rate limit
     */
    public static class RateLimitInfo {
        private final int limit;
        private final int remaining;
        private final long resetIn;

        public RateLimitInfo(int limit, int remaining, long resetIn) {
            this.limit = limit;
            this.remaining = remaining;
            this.resetIn = resetIn;
        }

        public int getLimit() {
            return limit;
        }

        public int getRemaining() {
            return remaining;
        }

        public long getResetIn() {
            return resetIn;
        }
    }
}
