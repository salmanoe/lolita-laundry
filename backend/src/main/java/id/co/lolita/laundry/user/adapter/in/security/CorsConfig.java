package id.co.lolita.laundry.user.adapter.in.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Single source of CORS configuration, shared by both the dev and prod security chains
 * (each picks it up via {@code http.cors(withDefaults())}). Always active so there is
 * exactly one {@link CorsConfigurationSource} bean regardless of profile.
 */
@Configuration
class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        // Vercel frontend origins — update with the production URL when the domain is registered
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",            // Vite dev server
                "https://*.vercel.app",             // Vercel preview deployments
                "https://lolita-laundry.vercel.app" // Vercel production
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Let the browser read the download filename on Excel exports (cross-origin in prod).
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}