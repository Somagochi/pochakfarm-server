package com.somagochi.pochakfarm.characterization.domain;

public record CharacterizerResult(
    String status,
    String provider,
    String fallbackFrom,
    String animalName,
    String contentType,
    String imageBase64,
    Integer elapsedMs) {}
