package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;

public record CaptureGenerationCommand(
    Long captureId,
    Long userId,
    String originalImageKey,
    String animalName,
    CardType cardType,
    Tier tier,
    CardSkill skill1,
    CardSkill skill2,
    String cardNo,
    long submittedAtNanos) {}
