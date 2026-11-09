package com.indoornav.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.indoornav.backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientAddressResolverTest {

    @Test
    void ignoresForwardedHeaderUnlessTrusted() {
        assertThat(resolverTrusting(false).resolve(request("198.51.100.1, 203.0.113.5")))
                .isEqualTo("10.0.0.1");
    }

    @Test
    void usesTheEntryAddedByTheTrustedProxyNotTheForgeableFirstOne() {
        assertThat(resolverTrusting(true).resolve(request("198.51.100.1, 203.0.113.5")))
                .isEqualTo("203.0.113.5");
    }

    @Test
    void fallsBackToTheConnectionAddressWhenTheHeaderIsAbsent() {
        assertThat(resolverTrusting(true).resolve(request(null))).isEqualTo("10.0.0.1");
    }

    private static MockHttpServletRequest request(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    private static ClientAddressResolver resolverTrusting(boolean trust) {
        return new ClientAddressResolver(new AppProperties(
                false,
                new AppProperties.Admin("admin", ""),
                new AppProperties.Jwt("", 60),
                new AppProperties.Login(5, 15, trust),
                new AppProperties.FloorPlans("")));
    }
}
