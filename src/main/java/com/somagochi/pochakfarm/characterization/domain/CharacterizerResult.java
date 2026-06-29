package com.somagochi.pochakfarm.characterization.domain;

public record CharacterizerResult(
    String status,
    String provider,
    String contentType,
    String aiImageBase64,
    String cardImageBase64,
    Integer elapsedMs) {}
