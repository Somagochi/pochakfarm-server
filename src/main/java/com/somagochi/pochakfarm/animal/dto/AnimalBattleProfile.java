package com.somagochi.pochakfarm.animal.dto;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;

public record AnimalBattleProfile(
    Long animalId,
    Long captureId,
    String animalName,
    CardType cardType,
    Tier tier,
    CardSkill skill1,
    CardSkill skill2) {}
