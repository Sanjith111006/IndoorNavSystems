package com.indoornav.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indoornav.backend.config.AppProperties;
import com.indoornav.backend.dto.LoginRequest;
import com.indoornav.backend.dto.LoginResponse;
import com.indoornav.backend.exception.InvalidCredentialsException;
import com.indoornav.backend.exception.LoginThrottledException;
import com.indoornav.backend.security.LoginThrottle;
import com.indoornav.backend.security.TokenService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String ADDRESS = "203.0.113.5";
    private static final String PASSWORD = "fake-test-password";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private TokenService tokenService;

    @Mock
    private LoginThrottle loginThrottle;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = serviceWithHash(encoder.encode(PASSWORD));
        lenient().when(loginThrottle.retryAfter(ADDRESS)).thenReturn(Optional.empty());
    }

    @Test
    void correctCredentialsIssueATokenAndClearFailures() {
        when(tokenService.issueAdminToken("admin"))
                .thenReturn(new TokenService.IssuedToken("signed", 3600));

        LoginResponse response = service.login(new LoginRequest("admin", PASSWORD), ADDRESS);

        assertThat(response.accessToken()).isEqualTo("signed");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
        verify(loginThrottle).clear(ADDRESS);
    }

    @Test
    void wrongPasswordAndWrongUsernameFailIdentically() {
        InvalidCredentialsException wrongPassword = catchInvalid(new LoginRequest("admin", "nope"));
        InvalidCredentialsException wrongUsername = catchInvalid(new LoginRequest("someone", PASSWORD));

        assertThat(wrongPassword.getMessage()).isEqualTo(wrongUsername.getMessage());
        verify(tokenService, never()).issueAdminToken(anyString());
    }

    @Test
    void failuresAreCounted() {
        catchInvalid(new LoginRequest("admin", "nope"));

        verify(loginThrottle).recordFailure(ADDRESS);
    }

    @Test
    void throttledAddressIsRejectedEvenWithCorrectCredentials() {
        when(loginThrottle.retryAfter(ADDRESS)).thenReturn(Optional.of(Duration.ofMinutes(3)));

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", PASSWORD), ADDRESS))
                .isInstanceOfSatisfying(
                        LoginThrottledException.class,
                        exception -> assertThat(exception.retryAfter()).isEqualTo(Duration.ofMinutes(3)));
        verify(tokenService, never()).issueAdminToken(anyString());
    }

    @Test
    void passwordLongerThanBcryptsInputLimitFailsCleanly() {
        catchInvalid(new LoginRequest("admin", "x".repeat(200)));
    }

    @Test
    void signInIsDisabledWhenNoHashIsConfigured() {
        AuthService withoutHash = serviceWithHash("");

        assertThatThrownBy(() -> withoutHash.login(new LoginRequest("admin", ""), ADDRESS))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private InvalidCredentialsException catchInvalid(LoginRequest request) {
        try {
            service.login(request, ADDRESS);
        } catch (InvalidCredentialsException exception) {
            return exception;
        }
        throw new AssertionError("Expected the login to be rejected");
    }

    private AuthService serviceWithHash(String hash) {
        AppProperties properties = new AppProperties(
                false,
                new AppProperties.Admin("admin", hash),
                new AppProperties.Jwt("", 60),
                new AppProperties.Login(5, 15, false),
                new AppProperties.FloorPlans(""));
        return new AuthService(encoder, tokenService, loginThrottle, properties);
    }
}
