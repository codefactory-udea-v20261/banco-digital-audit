package com.udea.bancodigital.audit.infrastructure.security;

import com.udea.bancodigital.shared.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final IdentityServiceClient identityServiceClient;

    public JwtAuthenticationFilter(IdentityServiceClient identityServiceClient) {
        this.identityServiceClient = identityServiceClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                TokenValidationResponse validationResponse = identityServiceClient.validateToken(jwt);
                if (validationResponse.isActive()) {
                    setAuthentication(validationResponse, request);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context: " + ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(TokenValidationResponse validationResponse, HttpServletRequest request) {
        UUID clienteId = parseUuid(validationResponse.getClienteId());
        UUID userId = parseUuid(validationResponse.getUid());

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                userId, validationResponse.getSub(), clienteId
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                validationResponse.getAuthorities().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private UUID parseUuid(String value) {
        return (value != null && !value.isEmpty()) ? UUID.fromString(value) : null;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
