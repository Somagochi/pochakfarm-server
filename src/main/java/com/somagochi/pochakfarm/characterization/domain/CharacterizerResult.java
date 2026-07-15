package com.somagochi.pochakfarm.characterization.domain;

public record CharacterizerResult(
    String status,
    String provider,
    String contentType,
    String cardImageBase64,
    Integer elapsedMs) {}
