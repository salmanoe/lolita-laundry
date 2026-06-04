package id.co.lolita.laundry.user.adapter.in.security;

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
class DevSecurityConfig {

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
