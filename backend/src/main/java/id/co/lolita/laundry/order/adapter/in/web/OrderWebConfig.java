package id.co.lolita.laundry.order.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link PublicRateLimitInterceptor} on the public order endpoints.
 */
@Configuration
@RequiredArgsConstructor
class OrderWebConfig implements WebMvcConfigurer {

    private final PublicRateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/public/**");
    }
}
