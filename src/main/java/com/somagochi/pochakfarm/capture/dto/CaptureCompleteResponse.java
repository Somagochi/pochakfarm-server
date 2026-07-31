package com.somagochi.pochakfarm.capture.dto;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;

public record CaptureCompleteResponse(Long captureId, GenerationStatus generationStatus) {

  public static CaptureCompleteResponse from(Capture capture) {
    return new CaptureCompleteResponse(capture.getId(), capture.getGenerationStatus());
  }
}
