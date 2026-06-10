package com.yoedu.yoedurealestateapi.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component


    public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private static final String AUTHORIZATION_HEADER = "Authorization";
        private static final String BEARER_PREFIX = "Bearer ";

        private final JwtService jwtService;

        public JwtAuthenticationFilter(JwtService jwtService) {
            this.jwtService = jwtService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            try {
                if (!jwtService.isAccessToken(token)) {
                    writeUnauthorizedResponse(response, "Invalid access token");
                    return;
                }

                String username = jwtService.extractUsername(token);
                List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(token).stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null,
                        authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                writeUnauthorizedResponse(response, "Token is invalid or expired");
                return;
            }

            filterChain.doFilter(request, response);
        }

        private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"" + escapeJson(message)
                    + "\",\"data\":null,\"timestamp\":null}");
        }

        private String escapeJson(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
