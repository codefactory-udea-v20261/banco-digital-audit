package com.udea.bancodigital.audit.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenValidationResponseTest {

    @Test
    void getAuthorities_givenAuthoritiesIsNull_returnsEmptyList() {
        TokenValidationResponse response = new TokenValidationResponse();

        response.setAuthorities(null);

        assertThat(response.getAuthorities()).isEmpty();
    }

    @Test
    void getAuthorities_givenAuthoritiesAreSet_returnsTheSameList() {
        TokenValidationResponse response = new TokenValidationResponse();
        List<String> authorities = List.of("ROLE_ADMIN", "READ");

        response.setAuthorities(authorities);

        assertThat(response.getAuthorities()).containsExactly("ROLE_ADMIN", "READ");
    }

    @Test
    void inactive_returnsResponseWithActiveFalse() {
        TokenValidationResponse response = TokenValidationResponse.inactive();

        assertThat(response.isActive()).isFalse();
    }
}
