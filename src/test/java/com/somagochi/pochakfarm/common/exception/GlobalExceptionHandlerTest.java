package com.somagochi.pochakfarm.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
  void handlesNoResourceFoundException() {
    NoResourceFoundException exception =
        new NoResourceFoundException(HttpMethod.GET, "/share/init_data", "share/init_data");

    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleNoResourceFoundException(exception);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals(404, response.getBody().status());
    assertEquals("NOT_FOUND", response.getBody().code());
    assertEquals("Resource not found", response.getBody().message());
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

  @Test
  void handlesEnumMethodArgumentTypeMismatchException() {
    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException(
            "river", CardType.class, "theme", null, new IllegalArgumentException("boom"));

    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleMethodArgumentTypeMismatchException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(400, response.getBody().status());
    assertEquals("INVALID_PARAMETER", response.getBody().code());
    assertEquals("theme must be one of [GROUND, SKY, SPACE, SEA]", response.getBody().message());
  }

  @Test
  void handlesNonEnumMethodArgumentTypeMismatchException() {
    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException(
            "abc", Long.class, "page", null, new IllegalArgumentException("boom"));

    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleMethodArgumentTypeMismatchException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("INVALID_PARAMETER", response.getBody().code());
    assertEquals("page has an invalid value", response.getBody().message());
  }
}
