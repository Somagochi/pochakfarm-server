package com.somagochi.pochakfarm.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
        exception.getMessage()
    );
  }

  @ExceptionHandler(Exception.class) 
  public ResponseEntity<ErrorResponse> handleException(Exception exception) {
    loggingError(exception);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "INTERNAL_SERVER_ERROR",
        "Unexpected server error"
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
