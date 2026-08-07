package com.somagochi.pochakfarm.characterization.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerResult;

public record HttpCaptureCharacterizerResponse(
    String status,
    String provider,
    @JsonProperty("animal_content_type") String animalContentType,
    @JsonProperty("card_content_type") String cardContentType,
    @JsonProperty("elapsed_ms") Integer elapsedMs) {

  public CaptureCharacterizerResult toResult() {
    return new CaptureCharacterizerResult(
        status, provider, animalContentType, cardContentType, elapsedMs);
  }
}
