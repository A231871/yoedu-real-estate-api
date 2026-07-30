package com.yoedu.yoedurealestateapi.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Auth header exists?
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                
                // Access token valid?
                final String token = authHeader.substring(BEARER_PREFIX.length());
                if (jwtService.validateToken(token, TokenType.ACCESS_TOKEN)) {
                    
                    // User Id exists?
                    String userId = jwtService.extractUserId(token);
                    if (userId != null) {
                        List<SimpleGrantedAuthority> authorities = jwtService
                            .extractRoles(token)
                            .stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList();

                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                        );

                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    } else {
                        logger.debug("No user Id from the access token");
                    }
                } else {
                    logger.debug("Access token invalid or expired");
                }
            } else {
                logger.debug("No Bearer Header, skip processing");
            }
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Failed to process JWT Token: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
