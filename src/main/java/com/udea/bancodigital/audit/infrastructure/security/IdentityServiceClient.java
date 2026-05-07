package com.udea.bancodigital.audit.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class IdentityServiceClient {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    public IdentityServiceClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${services.identity.url}") String identityServiceUrl
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.identityServiceUrl = identityServiceUrl;
    }

    public TokenValidationResponse validateToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(token, headers);
            TokenValidationResponse response = restTemplate.postForObject(
                    identityServiceUrl + "/api/v1/auth/validate-token",
                    request,
                    TokenValidationResponse.class
            );
            return response != null ? response : TokenValidationResponse.inactive();
        } catch (RestClientException ex) {
            return TokenValidationResponse.inactive();
        }
    }
}
