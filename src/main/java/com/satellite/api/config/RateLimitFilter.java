package com.satellite.api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiting filter using Bucket4j's token-bucket algorithm.
 *
 * <p>Each IP address gets its own bucket with a configurable number of
 * tokens per minute. When the bucket is exhausted, the filter returns
 * {@code 429 Too Many Requests} with a {@code Retry-After} header.
 *
 * <p>The limit is configured via {@code app.rate-limit.requests-per-minute}
 * in {@code application.properties} (default: 30).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public RateLimitFilter(@Value("${app.rate-limit.requests-per-minute:30}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
        log.info("Rate limiter initialised: {} requests/minute per IP", requestsPerMinute);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createBucket());

        if (bucket.tryConsume(1)) {
            // Add rate limit headers so the frontend can see remaining capacity
            response.addHeader("X-Rate-Limit-Limit", String.valueOf(requestsPerMinute));
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("Retry-After", "60");
            response.getWriter().write(
                    "{\"message\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    /**
     * Only apply rate limiting to API endpoints, not Swagger UI or static resources.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the real client IP, accounting for reverse proxies (Railway, etc.)
     * that set the {@code X-Forwarded-For} header.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain multiple IPs; the first is the real client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
