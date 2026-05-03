package com.udea.bancodigital.shared.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String username,
        UUID clienteId
) {
}
