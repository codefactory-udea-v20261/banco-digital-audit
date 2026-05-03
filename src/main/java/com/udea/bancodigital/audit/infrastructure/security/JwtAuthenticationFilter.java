package com.udea.bancodigital.audit.infrastructure.security;

import com.udea.bancodigital.shared.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
                Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.get("roles");
                    @SuppressWarnings("unchecked")
                    List<String> permissions = (List<String>) claims.get("permissions");
                    
                    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    if (roles != null) {
                        authorities.addAll(roles.stream()
                                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                                .collect(Collectors.toList()));
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_CLIENTE"));
                    }
                    if (permissions != null) {
                        authorities.addAll(permissions.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()));
                    }

                    String clienteIdStr = (String) claims.get("clienteId");
                    UUID clienteId = (clienteIdStr != null && !clienteIdStr.isEmpty()) ? UUID.fromString(clienteIdStr) : null;
                    String userIdStr = (String) claims.get("uid");
                    UUID userId = (userIdStr != null && !userIdStr.isEmpty()) ? UUID.fromString(userIdStr) : null;

                    AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                        userId,
                        claims.getSubject(),
                        clienteId
                    );

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        authorities
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context: " + ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
