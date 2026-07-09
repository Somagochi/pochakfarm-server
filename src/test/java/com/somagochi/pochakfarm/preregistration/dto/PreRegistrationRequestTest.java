package com.somagochi.pochakfarm.preregistration.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PreRegistrationRequestTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void deserializesCharacterizationIdFromSnakeCaseRequestBody() throws Exception {
    String json =
        """
        {
          "phoneNumber": "010-1234-5678",
          "requiredConsent": true,
          "characterization_id": 10
        }
        """;

    PreRegistrationRequest request = objectMapper.readValue(json, PreRegistrationRequest.class);

    assertEquals("010-1234-5678", request.phoneNumber());
    assertEquals(true, request.requiredConsent());
    assertEquals(10L, request.characterizationId());
  }
}
