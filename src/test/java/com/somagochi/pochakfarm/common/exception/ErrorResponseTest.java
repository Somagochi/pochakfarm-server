package com.somagochi.pochakfarm.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ErrorResponseTest {

  @Test
  void createsErrorResponseWithCurrentTimestamp() {
    ErrorResponse response = ErrorResponse.of(
        400,
        "BAD_REQUEST",
        "Invalid request"
    );

    assertNotNull(response.timestamp());
    assertEquals(400, response.status());
    assertEquals("BAD_REQUEST", response.code());
    assertEquals("Invalid request", response.message());
  }
}
