package com.somagochi.pochakfarm.develop.dto;

import com.somagochi.pochakfarm.battle.domain.GymLeaderAnimal;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;

public record DevelopGymLeaderAnimalView(
    Long id,
    Integer orderNo,
    String animalName,
    CardType cardType,
    String cardTypeLabel,
    Tier tier,
    String skill1Name,
    SkillBattleType skill1BattleType,
    String skill2Name,
    SkillBattleType skill2BattleType,
    String imageKey,
    String imageUrl) {

  public static DevelopGymLeaderAnimalView of(GymLeaderAnimal animal, String imageUrl) {
    CardSkill skill1 = animal.getSkill1();
    CardSkill skill2 = animal.getSkill2();
    return new DevelopGymLeaderAnimalView(
        animal.getId(),
        animal.getOrderNo(),
        animal.getAnimalName(),
        animal.getCardType(),
        animal.getCardType().label(),
        animal.getTier(),
        skill1.displayName(),
        skill1.battleType(),
        skill2.displayName(),
        skill2.battleType(),
        animal.getImageKey(),
        imageUrl);
  }
}
