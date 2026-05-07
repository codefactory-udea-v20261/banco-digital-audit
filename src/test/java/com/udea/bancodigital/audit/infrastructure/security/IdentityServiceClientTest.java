package com.udea.bancodigital.audit.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityServiceClientTest {

    private static final String IDENTITY_URL = "http://identity:8081";

    private RestTemplate restTemplate;
    private IdentityServiceClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
        client = new IdentityServiceClient(builder, IDENTITY_URL);
    }

    @Test
    void validateToken_givenIdentityReturnsActiveResponse_returnsThatResponse() {
        TokenValidationResponse expected = new TokenValidationResponse();
        expected.setActive(true);
        expected.setSub("user@banco.com");
        when(restTemplate.postForObject(
                contains("/api/v1/auth/validate-token"),
                any(),
                eq(TokenValidationResponse.class)
        )).thenReturn(expected);

        TokenValidationResponse result = client.validateToken("a-jwt");

        assertThat(result).isSameAs(expected);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void validateToken_givenIdentityReturnsNullBody_returnsInactiveResponse() {
        when(restTemplate.postForObject(
                contains("/api/v1/auth/validate-token"),
                any(),
                eq(TokenValidationResponse.class)
        )).thenReturn(null);

        TokenValidationResponse result = client.validateToken("a-jwt");

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void validateToken_givenRestClientException_returnsInactiveResponse() {
        when(restTemplate.postForObject(
                contains("/api/v1/auth/validate-token"),
                any(),
                eq(TokenValidationResponse.class)
        )).thenThrow(new ResourceAccessException("identity unreachable"));

        TokenValidationResponse result = client.validateToken("a-jwt");

        assertThat(result.isActive()).isFalse();
    }
}
