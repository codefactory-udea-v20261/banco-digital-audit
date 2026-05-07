package com.udea.bancodigital.audit.infrastructure.security;

import com.udea.bancodigital.shared.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private IdentityServiceClient identityServiceClient;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should populate security context when identity service validates the JWT")
    void shouldPopulateSecurityContext() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        TokenValidationResponse validationResponse = new TokenValidationResponse();
        validationResponse.setActive(true);
        validationResponse.setSub("test@banco.com");
        validationResponse.setUid(userId.toString());
        validationResponse.setClienteId(clienteId.toString());
        validationResponse.setAuthorities(List.of("ROLE_ADMIN", "READ", "WRITE"));

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(identityServiceClient.validateToken("valid-token")).thenReturn(validationResponse);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.username()).isEqualTo("test@banco.com");
        assertThat(user.clienteId()).isEqualTo(clienteId);
        assertThat(authentication.getAuthorities()).hasSize(3);
    }

    @Test
    @DisplayName("Should continue filter chain and not set context when no JWT")
    void shouldContinueWhenNoJwt() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should continue without authentication when identity service marks token inactive")
    void shouldHandleInactiveToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(identityServiceClient.validateToken("invalid-token")).thenReturn(TokenValidationResponse.inactive());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
