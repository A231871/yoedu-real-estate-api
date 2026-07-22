package com.yoedu.yoedurealestateapi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a consistent ApiResponse-formatted JSON 403 when an authenticated
 * user lacks the required authority. Handles filter-level access denials
 * (e.g., missing ROLE) which bypass @RestControllerAdvice entirely.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Access denied. You do not have permission to access this resource.\",\"data\":null,\"timestamp\":\"" + java.time.Instant.now() + "\"}");
    }
}
