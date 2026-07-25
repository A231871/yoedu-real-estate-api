package com.yoedu.yoedurealestateapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoedu.yoedurealestateapi.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Returns a consistent ApiResponse-formatted JSON 403 when an authenticated
 * user lacks the required authority. Handles filter-level access denials
 * (e.g., missing ROLE) which bypass @RestControllerAdvice entirely.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
            response.getWriter(),
            ApiResponse.error(
                "Access denied. You do not have permission to access this resource."
            )
        );
    }
}
