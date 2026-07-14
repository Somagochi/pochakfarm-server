package com.somagochi.pochakfarm.characterization.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;

public record HttpCharacterizerResponse(
    String status,
    String provider,
    @JsonProperty("content_type") String contentType,
    @JsonProperty("card_image_base64") String cardImageBase64,
    @JsonProperty("elapsed_ms") Integer elapsedMs) {

  public CharacterizerResult toResult() {
    return new CharacterizerResult(status, provider, contentType, cardImageBase64, elapsedMs);
  }
}
