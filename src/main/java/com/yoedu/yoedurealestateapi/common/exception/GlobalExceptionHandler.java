package com.yoedu.yoedurealestateapi.common.exception;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ──────────────────────────────────────────────────────────────────────────
    // Domain exceptions
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiResponse.error(ex.getMessage())
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiResponse.error(ex.getMessage())
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(
            ApiResponse.error(ex.getMessage())
        );
    }

    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserBanned(UserBannedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Validation exceptions
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Handles @Valid failures on @RequestBody. Standardised to return an
     * ApiResponse envelope so clients see a consistent error format.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        // Field-level errors (e.g., @NotBlank, @Size)
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // Class-level / global errors (e.g., cross-field validators)
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            errors.putIfAbsent(globalError.getObjectName(), globalError.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Validation failed", errors, java.time.Instant.now())
        );
    }

    /**
     * Handles @Validated failures on service/method parameters.
     * Scrubs internal class paths from the violation message.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            // Strip class/method prefix — keep only the leaf field name
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            errors.putIfAbsent(field, violation.getMessage());
        }
        return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Validation failed", errors, java.time.Instant.now())
        );
    }

    /**
     * Invalid path variable types (e.g., non-UUID where UUID is expected).
     * Returns 400 instead of falling through to the 500 catch-all.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format(
                "Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    /**
     * JPA/Hibernate validation failures that are wrapped in a TransactionSystemException
     * when a transaction is being committed.
     */
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<Object> handleTransactionSystem(TransactionSystemException ex) {
        Throwable cause = ex.getRootCause();
        if (cause instanceof ConstraintViolationException cve) {
            return ResponseEntity.badRequest().body(handleConstraintViolation(cve).getBody());
        }
        log.error("Unhandled TransactionSystemException", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("An unexpected error occurred. Please try again later.")
        );
    }

    /**
     * Handles business/domain argument validation failures (e.g., bad conversation parameters).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Security exceptions
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiResponse.error("Username or password is invalid")
        );
    }

    /**
     * Handles java.lang.SecurityException thrown by domain service assertions.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleJavaSecurityException(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error(ex.getMessage() != null ? ex.getMessage() : "Access denied. You do not have permission to perform this action."));
    }

    /**
     * Handles method-security (e.g., @PreAuthorize) access denials that reach
     * the MVC layer. Filter-level denials are handled by JwtAccessDeniedHandler.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error("Access denied. You do not have permission to access this resource."));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Database exceptions
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        if (message != null && message.contains("no_listing_double_booking")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            "Khoảng thời gian này đã có lịch hẹn khác cho tin đăng này. Vui lòng chọn thời gian khác."));
        }

        if (message != null && message.contains("favorites_user_id_listing_id_key")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Tin đăng này đã có trong danh sách yêu thích"));
        }

        if (message != null && message.contains("idx_users_email_unique")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Email này đã được sử dụng. Vui lòng chọn email khác."));
        }

        if (message != null && message.contains("idx_reviews_unique_schedule")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            "A review for this viewing schedule already exists."));
        }

        if (message != null && message.contains("idx_reviews_one_per_user")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            "You have already reviewed this listing."));
        }

        // Unknown constraint violation — log internally, return opaque 500
        log.error("Unhandled DataIntegrityViolationException", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("An unexpected error occurred. Please try again later.")
        );
    }

    /**
     * Maps @Version optimistic locking failures to HTTP 409 Conflict.
     * This protects against concurrent updates on ViewingSchedule and other versioned entities.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                    "The record was modified by another user. Please refresh and try again."));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Spring MVC framework exceptions
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called by ResponseEntityExceptionHandler for Spring MVC exceptions such as:
     * - 405 HttpRequestMethodNotSupportedException
     * - 415 HttpMediaTypeNotSupportedException
     * - 400 HttpMessageNotReadableException
     * Wraps them in ApiResponse so clients get a consistent error envelope.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message;
        if (status.is4xxClientError()) {
            // Client errors: safe to surface the message
            message = ex.getMessage() != null ? ex.getMessage() : "Bad request";
        } else {
            // Server errors: log detail internally, return generic message
            log.error("Spring MVC server error [{}]", status.value(), ex);
            message = "An unexpected error occurred. Please try again later.";
        }

        return ResponseEntity.status(status).headers(headers).body(
                ApiResponse.error(message)
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Catch-all
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Last-resort handler. Logs the full exception internally and returns a
     * generic message — never exposes raw exception details to clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception caught by GlobalExceptionHandler", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiResponse.error("An unexpected error occurred. Please try again later.")
        );
    }
}
