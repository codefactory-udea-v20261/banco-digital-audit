package com.udea.bancodigital.audit.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain filterChain;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Should initialize SecurityFilterChain bean")
    void shouldCreateSecurityFilterChain() {
        assertThat(filterChain).isNotNull();
    }

    @Test
    @DisplayName("Should create PasswordEncoder bean")
    void shouldCreatePasswordEncoder() {
        assertThat(passwordEncoder).isNotNull();
    }
}
