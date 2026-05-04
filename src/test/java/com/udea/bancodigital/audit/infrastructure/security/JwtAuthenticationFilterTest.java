package com.udea.bancodigital.audit.infrastructure.security;

import com.udea.bancodigital.shared.security.AuthenticatedUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private static final String SECRET = "A_VERY_LONG_SECRET_KEY_FOR_TESTING_PURPOSES_OVER_256_BITS";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should populate security context when valid JWT is provided")
    void shouldPopulateSecurityContext() throws ServletException, IOException {
        // Given
        UUID userId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        String jwt = generateToken(userId, clienteId, List.of("ADMIN"), List.of("READ", "WRITE"));

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.username()).isEqualTo("test@banco.com");
        assertThat(user.clienteId()).isEqualTo(clienteId);
        
        assertThat(authentication.getAuthorities()).hasSize(3); // ROLE_ADMIN, READ, WRITE
    }

    @Test
    @DisplayName("Should continue filter chain and not set context when no JWT")
    void shouldContinueWhenNoJwt() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    
    @Test
    @DisplayName("Should set default ROLE_CLIENTE when roles are null")
    void shouldSetDefaultRole() throws ServletException, IOException {
        // Given
        UUID userId = UUID.randomUUID();
        String jwt = generateToken(userId, null, null, null);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        
        assertThat(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"))).isTrue();
    }
    
    @Test
    @DisplayName("Should not throw exception when JWT is invalid")
    void shouldHandleInvalidJwt() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should handle empty strings and ROLE_ prefix correctly")
    void shouldHandleEmptyStringsAndRolePrefix() throws ServletException, IOException {
        // Given
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String jwt = Jwts.builder()
                .subject("test@banco.com")
                .claim("uid", "") // Empty string
                .claim("clienteId", "") // Empty string
                .claim("roles", List.of("ROLE_USER")) // Has prefix
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 100000))
                .signWith(key)
                .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        assertThat(user.userId()).isNull();
        assertThat(user.clienteId()).isNull();
        
        assertThat(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))).isTrue();
    }

    private String generateToken(UUID userId, UUID clienteId, List<String> roles, List<String> permissions) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder()
                .subject("test@banco.com")
                .claim("uid", userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 100000))
                .signWith(key);

        if (clienteId != null) builder.claim("clienteId", clienteId.toString());
        if (roles != null) builder.claim("roles", roles);
        if (permissions != null) builder.claim("permissions", permissions);

        return builder.compact();
    }
}
