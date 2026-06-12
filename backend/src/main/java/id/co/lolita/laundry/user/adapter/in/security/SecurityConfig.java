package id.co.lolita.laundry.user.adapter.in.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import lombok.RequiredArgsConstructor;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Profile("!dev")  // replaced by DevSecurityConfig in dev profile
class SecurityConfig {

    private final Auth0JwtAuthenticationConverter jwtConverter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)           // stateless API — no CSRF needed
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(withDefaults())   // uses the CorsConfigurationSource bean from CorsConfig
                .authorizeHttpRequests(auth -> auth
                        // Let Spring's internal error dispatch reach /error without a JWT; otherwise any
                        // exception on a permitAll endpoint is re-dispatched to /error and masked as a 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // All requests require a valid Auth0 JWT — order entry is now in-house
                        // (DAILY_STAFF); the public tokenized order form has been retired.
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                )
                .build();
    }
}
