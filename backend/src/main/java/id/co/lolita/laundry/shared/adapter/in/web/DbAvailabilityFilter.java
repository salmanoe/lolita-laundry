package id.co.lolita.laundry.shared.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Renders a clean {@code 503 Service Unavailable} when the database is unreachable, instead of a
 * raw {@code 500}.
 *
 * <p>The database is read inside the Spring Security JWT→authorities converter
 * ({@code Auth0JwtAuthenticationConverter}), which runs in the servlet filter chain <em>before</em>
 * the {@code DispatcherServlet}. A connection failure thrown there never reaches
 * {@link GlobalExceptionHandler} (a {@code @RestControllerAdvice} only sees MVC-dispatched
 * requests), so it would surface as an unhandled 500. This filter wraps the whole chain — it is
 * ordered ahead of Spring Security's {@code FilterChainProxy} (order -100) — and translates a
 * DB-unreachable failure that bubbles out (from the security converter <em>or</em> a controller)
 * into a friendly 503.
 *
 * <p>Catches {@link DataAccessResourceFailureException} (query-time connection loss, e.g.
 * {@code CannotGetJdbcConnectionException}) and {@link CannotCreateTransactionException} (no
 * connection to open the repository transaction). Deliberately narrow — it does <em>not</em> catch
 * bad-SQL / integrity {@code DataAccessException}s, which are genuine bugs and should stay 4xx/500.
 *
 * <p>This is the failure mode behind the 2026-07 login outage: Neon's free-tier compute quota was
 * exhausted, so every {@code loadByAuth0Sub} lookup failed and {@code /api/me} 500'd for every
 * authenticated caller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DbAvailabilityFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (DataAccessResourceFailureException | CannotCreateTransactionException ex) {
            log.error("Database unavailable while handling {} {}", request.getMethod(), request.getRequestURI(), ex);
            if (response.isCommitted()) {
                throw ex; // too late to rewrite the response — let it propagate
            }
            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                    "Layanan sedang tidak tersedia. Silakan coba lagi beberapa saat lagi.");
            problem.setTitle("Service Unavailable");
            response.reset();
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), problem);
        }
    }
}
