package com.somagochi.pochakfarm.capture.domain;

public record CaptureCharacterizerResult(
    String status,
    String provider,
    String sceneContentType,
    String cardContentType,
    Integer elapsedMs) {}
