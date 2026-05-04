package com.udea.bancodigital.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    @Test
    @DisplayName("Should create AuthenticatedUser record correctly")
    void testAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        String username = "testuser";
        UUID clienteId = UUID.randomUUID();

        AuthenticatedUser user = new AuthenticatedUser(userId, username, clienteId);

        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.username()).isEqualTo(username);
        assertThat(user.clienteId()).isEqualTo(clienteId);
    }
}
