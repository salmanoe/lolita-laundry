package id.co.lolita.laundry.order.adapter.in.web;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP in-memory rate limit for the public order endpoints, so a leaked token can't be
 * used to spam orders. Each client IP gets a token bucket of {@value #CAPACITY} requests
 * that refills over one minute.
 *
 * <p>In-memory is sufficient for a single-VM deployment. If the app is ever horizontally
 * scaled, swap in a distributed Bucket4j backend (e.g. Redis).
 */
@Component
class PublicRateLimitInterceptor implements HandlerInterceptor {

    private static final int CAPACITY = 20;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {
        var bucket = buckets.computeIfAbsent(clientIp(request), _ -> newBucket());
        if (bucket.tryConsume(1)) {
            return true;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(
                "{\"title\":\"Too Many Requests\",\"status\":429,"
                        + "\"detail\":\"Rate limit exceeded. Please try again shortly.\"}");
        return false;
    }

    private static Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(CAPACITY).refillGreedy(CAPACITY, Duration.ofMinutes(1)))
                .build();
    }

    private static String clientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
