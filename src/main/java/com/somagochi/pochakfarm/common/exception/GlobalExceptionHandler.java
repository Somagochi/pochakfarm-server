package com.somagochi.pochakfarm.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String BAD_REQUEST_MESSAGE = "Invalid request";
  private static final String UNAUTHORIZED_MESSAGE = "Authentication is required";
  private static final String FORBIDDEN_MESSAGE = "Access is denied";
  private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Unexpected server error";

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
    loggingError(exception);
    return buildResponse(
        exception.getStatus(),
        exception.getCode(),
        exception.getMessage()
    );
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException exception
  ) {
    loggingError(exception);
    return buildResponse(
        HttpStatus.BAD_REQUEST.value(),
        "BAD_REQUEST",
        BAD_REQUEST_MESSAGE
    );
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException exception
  ) {
    loggingError(exception);
    return buildResponse(
        HttpStatus.UNAUTHORIZED.value(),
        "UNAUTHORIZED",
        UNAUTHORIZED_MESSAGE
    );
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException exception
  ) {
    loggingError(exception);
    return buildResponse(
        HttpStatus.FORBIDDEN.value(),
        "FORBIDDEN",
        FORBIDDEN_MESSAGE
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception exception) {
    loggingError(exception);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "INTERNAL_SERVER_ERROR",
        INTERNAL_SERVER_ERROR_MESSAGE
    );
  }

  private void loggingError(Exception e) {
    log.error("Error: {}", e.getMessage(), e);
  }

  private ResponseEntity<ErrorResponse> buildResponse(int status, String code, String message) {
    return ResponseEntity
        .status(HttpStatusCode.valueOf(status))
        .body(ErrorResponse.of(status, code, message));
  }
}
