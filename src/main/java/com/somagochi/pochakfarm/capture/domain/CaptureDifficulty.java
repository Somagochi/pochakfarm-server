package com.somagochi.pochakfarm.capture.domain;

public record CaptureDifficulty(
    int roundDurationMs, int maxThrows, int ringShrinkDurationMs, int successWindowMs) {}
