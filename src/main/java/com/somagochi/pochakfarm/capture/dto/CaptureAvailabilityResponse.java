package com.somagochi.pochakfarm.capture.dto;

import java.time.Instant;

public record CaptureAvailabilityResponse(
    FreeAttempts freeAttempts, long extraCaptureCost, long coins, boolean canStartCapture) {

  public record FreeAttempts(int dailyLimit, int used, int remaining, Instant resetsAt) {}
}
