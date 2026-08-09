package com.somagochi.pochakfarm.capture.dto;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.CaptureDifficulty;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import java.time.Instant;

public record CaptureStartResponse(
    Long captureId,
    Tier tier,
    CardType cardType,
    CaptureDifficulty difficulty,
    Upload upload,
    Attempts attempts,
    Instant gameResultExpiresAt) {

  public static CaptureStartResponse from(
      Capture capture, CaptureDifficulty difficulty, PresignResponse presign, int remaining) {
    return new CaptureStartResponse(
        capture.getId(),
        capture.getTier(),
        capture.getCardType(),
        difficulty,
        new Upload(presign.uploadUrl(), presign.key(), presign.expiresAt()),
        new Attempts(remaining),
        capture.getGameResultExpiresAt());
  }

  public record Upload(String url, String key, Instant expiresAt) {}

  public record Attempts(int remaining) {}
}
