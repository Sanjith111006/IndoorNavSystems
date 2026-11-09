package com.indoornav.backend.controller;

import com.indoornav.backend.dto.LoginRequest;
import com.indoornav.backend.dto.LoginResponse;
import com.indoornav.backend.security.ClientAddressResolver;
import com.indoornav.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ClientAddressResolver clientAddressResolver;

    public AuthController(AuthService authService, ClientAddressResolver clientAddressResolver) {
        this.authService = authService;
        this.clientAddressResolver = clientAddressResolver;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, clientAddressResolver.resolve(httpRequest));
    }
}
