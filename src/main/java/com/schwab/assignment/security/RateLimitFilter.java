package com.schwab.assignment.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance safety limit. Deployments should move this concern to a gateway or shared store.
 */
@Component
class RateLimitFilter extends OncePerRequestFilter {
    private final int limit;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    RateLimitFilter(@Value("${app.rate-limit.requests-per-minute:60}") int limit) {
        this.limit = limit;
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String client = request.getRemoteAddr();
        Bucket bucket = buckets.compute(client, (key, current) -> current == null || current.started.plusSeconds(60).isBefore(Instant.now()) ? new Bucket(Instant.now(), 1) : current.increment());
        if (bucket.count > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"rate limit exceeded\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private record Bucket(Instant started, int count) {
        Bucket increment() {
            return new Bucket(started, count + 1);
        }
    }
}
