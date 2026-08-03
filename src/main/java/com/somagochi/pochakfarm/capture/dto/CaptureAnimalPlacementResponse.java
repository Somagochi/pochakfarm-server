package com.somagochi.pochakfarm.capture.dto;

import com.somagochi.pochakfarm.characterization.domain.CardType;

public record CaptureAnimalPlacementResponse(
    Long animalId,
    Long captureId,
    CardType cardType,
    Integer floorNum,
    Integer slotNum,
    String animalImageUrl) {}
