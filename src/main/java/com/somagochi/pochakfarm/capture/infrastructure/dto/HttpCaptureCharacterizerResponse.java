package com.somagochi.pochakfarm.capture.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerResult;

public record HttpCaptureCharacterizerResponse(
    String status,
    String provider,
    @JsonProperty("scene_content_type") String sceneContentType,
    @JsonProperty("card_content_type") String cardContentType,
    @JsonProperty("elapsed_ms") Integer elapsedMs) {

  public CaptureCharacterizerResult toResult() {
    return new CaptureCharacterizerResult(
        status, provider, sceneContentType, cardContentType, elapsedMs);
  }
}
