package com.yoedu.yoedurealestateapi.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleConflict_Returns409() {
        ConflictException ex = new ConflictException("Conflict message");
        ResponseEntity<ApiResponse<Void>> response = handler.handleConflict(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().success());
        assertEquals("Conflict message", response.getBody().message());
    }

    @Test
    void handleBadCredentials_Returns401() {
        BadCredentialsException ex = new BadCredentialsException("Invalid credentials");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().success());
        assertEquals("Username or password is invalid", response.getBody().message());
    }

    @Test
    void handleUnknown_ReturnsGenericMessage_NotRawException() {
        RuntimeException ex = new RuntimeException("secret db connection string");
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnknown(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().success());
        // Must NOT expose the raw exception message to clients
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().message());
    }

    @Test
    void handleTypeMismatch_Returns400() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("not-a-uuid", java.util.UUID.class, "listingId", null, null);

        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().success());
    }

    @Test
    void handleNotFound_Returns404() {
        NotFoundException ex = new NotFoundException("Resource not found");
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Resource not found", response.getBody().message());
    }
}
