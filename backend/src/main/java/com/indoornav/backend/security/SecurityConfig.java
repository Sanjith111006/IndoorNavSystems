package com.indoornav.backend.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The authorization matrix from API.md Section 3.2. Anything not listed here is denied.
 *
 * <p>Public paths are open to every method so that a wrong method reaches Spring MVC and returns
 * 405; only the documented methods have handlers.
 */
@Configuration
public class SecurityConfig {

    private static final String ROLE_CLAIM = "role";
    private static final String OBSTACLES_PATH = "/api/obstacles";

    private static final String[] PUBLIC_PATHS = {
        "/api/health",
        "/api/buildings",
        "/api/buildings/*",
        "/api/buildings/*/floors",
        "/api/buildings/*/nodes",
        "/api/floors/*/map",
        "/api/points-of-interest",
        "/api/points-of-interest/categories",
        "/api/navigation/route",
        "/api/auth/login",
        "/assets/floorplans/**"
    };

    private static final String[] API_DOCUMENTATION_PATHS = {
        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityErrorHandlers errorHandlers,
            BearerTokenResolver bearerTokenResolver,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            @Value("${springdoc.api-docs.enabled:false}") boolean apiDocumentationEnabled)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // RequestIdFilter sets the exact Cache-Control value the API contract requires.
                .headers(headers -> headers.cacheControl(cache -> cache.disable()))
                .authorizeHttpRequests(requests -> {
                    requests.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    requests.requestMatchers(PUBLIC_PATHS).permitAll();
                    if (apiDocumentationEnabled) {
                        requests.requestMatchers(API_DOCUMENTATION_PATHS).permitAll();
                    }
                    requests.requestMatchers(OBSTACLES_PATH, OBSTACLES_PATH + "/**").hasRole("ADMIN");
                    requests.anyRequest().denyAll();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandlers)
                        .accessDeniedHandler(errorHandlers))
                .oauth2ResourceServer(server -> server
                        .bearerTokenResolver(bearerTokenResolver)
                        .authenticationEntryPoint(errorHandlers)
                        .accessDeniedHandler(errorHandlers)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    /**
     * Reads tokens only on admin endpoints. The app attaches its token to every call, and a token
     * that has expired must not turn a public request into a 401.
     */
    @Bean
    BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        return request -> request.getRequestURI().startsWith(OBSTACLES_PATH)
                ? resolver.resolve(request)
                : null;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
