package id.co.lolita.laundry.user.adapter.in.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Development-only security configuration.
 *
 * <p>Permits all requests without JWT enforcement so the frontend can be developed
 * and tested locally before an Auth0 account is set up. This bean is active only
 * when {@code spring.profiles.active=dev} and is completely absent in production.
 *
 * <p>Active when: {@code SPRING_PROFILES_ACTIVE=dev} (the default in application.yaml).
 * Inactive when: any other profile (prod, test, etc.) — replaced by {@link SecurityConfig}.
 */
@Configuration
@EnableWebSecurity
@Profile("dev")
@Slf4j
class DevSecurityConfig {

    // Loud, unmissable log line whenever the permit-all chain is wired. The packaged JAR has no
    // default profile, so this can only appear when someone explicitly ran with the dev profile —
    // if it ever shows up in a production log, the app is serving unauthenticated and must be
    // restarted with SPRING_PROFILES_ACTIVE=prod.
    @PostConstruct
    void warnPermitAll() {
        log.warn("╔══════════════════════════════════════════════════════════════════════╗");
        log.warn("║  DEV SECURITY ACTIVE — ALL REQUESTS ARE PERMITTED WITHOUT A JWT.      ║");
        log.warn("║  This is local-development mode only. If you see this in production,  ║");
        log.warn("║  stop the app and restart with SPRING_PROFILES_ACTIVE=prod.           ║");
        log.warn("╚══════════════════════════════════════════════════════════════════════╝");
    }

    @Bean
    SecurityFilterChain devSecurityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(withDefaults())   // uses the CorsConfigurationSource bean from CorsConfig
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
