package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;

public record CaptureCharacterizerRequest(
    String originalImageDownloadUrl,
    String animalImageUploadUrl,
    String cardImageUploadUrl,
    String animalName,
    CardType cardType,
    Tier tier,
    CardSkill skill1,
    CardSkill skill2,
    String cardNo) {}
