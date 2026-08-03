package com.somagochi.pochakfarm.capture.dto;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;

public record CaptureResponse(
    Long captureId,
    Tier tier,
    CardType cardType,
    GenerationStatus generationStatus,
    GameStatus gameStatus,
    String sceneImageUrl,
    String cardImageUrl,
    String animalImageUrl,
    Integer elapsedMs,
    String failureReason) {

  public static CaptureResponse from(
      Capture capture,
      GameStatus gameStatus,
      String sceneImageUrl,
      String cardImageUrl,
      String animalImageUrl) {
    return new CaptureResponse(
        capture.getId(),
        capture.getTier(),
        capture.getCardType(),
        capture.getGenerationStatus(),
        gameStatus,
        sceneImageUrl,
        cardImageUrl,
        animalImageUrl,
        capture.getElapsedMs(),
        capture.getFailureReason());
  }
}
