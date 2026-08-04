package com.somagochi.pochakfarm.capture.dto;

import java.time.Instant;

public record CaptureAttemptPurchaseResponse(
    int remaining, long chargedCoins, long currentCoins, Instant resetsAt) {}
