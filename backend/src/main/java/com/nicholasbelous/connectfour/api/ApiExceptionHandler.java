package com.nicholasbelous.connectfour.api;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.nicholasbelous.connectfour.game.IllegalMoveException;
import com.nicholasbelous.connectfour.service.GameNotFoundException;
import com.nicholasbelous.connectfour.service.MoveConflictException;

/**
 * Turns every error into the same JSON shape: {@code {status, error, message}}.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so Spring MVC's own errors (malformed
 * body, failed validation, unknown route, wrong method, ...) use that shape too.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(GameNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(GameNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MoveConflictException.class)
    ResponseEntity<ErrorResponse> handleConflict(MoveConflictException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalMoveException.class)
    ResponseEntity<ErrorResponse> handleIllegalMove(IllegalMoveException e) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    /** Replaces the ProblemDetail body Spring MVC builds for its own errors. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(ErrorResponse.of(statusCode, messageFor(ex, body)));
    }

    private static String messageFor(Exception ex, Object body) {
        if (ex instanceof MethodArgumentNotValidException invalid) {
            return invalid.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Request body is missing or malformed";
        }
        if (body instanceof ProblemDetail problem && problem.getDetail() != null) {
            return problem.getDetail();
        }
        return ex.getMessage();
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }
}
