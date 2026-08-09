package com.somagochi.pochakfarm.capture.domain;

public record CaptureCharacterizerResult(
    String status,
    String provider,
    String animalContentType,
    String cardContentType,
    Integer elapsedMs) {}
