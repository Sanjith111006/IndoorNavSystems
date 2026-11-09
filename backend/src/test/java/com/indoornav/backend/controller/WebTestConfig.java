package com.indoornav.backend.controller;

import com.indoornav.backend.config.AppProperties;
import com.indoornav.backend.config.ClockConfig;
import com.indoornav.backend.exception.ErrorResponseFactory;
import com.indoornav.backend.exception.ErrorResponseWriter;
import com.indoornav.backend.security.ClientAddressResolver;
import com.indoornav.backend.security.JwtConfig;
import com.indoornav.backend.security.SecurityConfig;
import com.indoornav.backend.security.SecurityErrorHandlers;
import com.indoornav.backend.security.TokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** The real security and error beans, so web-slice tests exercise the actual contract. */
@TestConfiguration
@EnableConfigurationProperties(AppProperties.class)
@Import({
    SecurityConfig.class,
    JwtConfig.class,
    ClockConfig.class,
    TokenService.class,
    ErrorResponseFactory.class,
    ErrorResponseWriter.class,
    SecurityErrorHandlers.class,
    ClientAddressResolver.class
})
class WebTestConfig {
}
