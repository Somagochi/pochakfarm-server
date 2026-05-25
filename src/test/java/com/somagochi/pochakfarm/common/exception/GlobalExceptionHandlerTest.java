package com.somagochi.pochakfarm.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void handlesIllegalArgumentException() {
    IllegalArgumentException exception = new IllegalArgumentException("invalid parameter");

    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleIllegalArgumentException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(400, response.getBody().status());
    assertEquals("BAD_REQUEST", response.getBody().code());
    assertEquals("Invalid request", response.getBody().message());
  }

  @Test
  void handlesUnexpectedException() {
    RuntimeException exception = new RuntimeException("unexpected");

    ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(500, response.getBody().status());
    assertEquals("INTERNAL_SERVER_ERROR", response.getBody().code());
    assertEquals("Unexpected server error", response.getBody().message());
  }
}
