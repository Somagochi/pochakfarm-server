package com.somagochi.pochakfarm.capture.dto;

import java.time.Instant;

public record CaptureAvailabilityResponse(Attempts attempts, long attemptPurchaseCost, long coins) {

  public record Attempts(int remaining, Instant resetsAt) {}
}
