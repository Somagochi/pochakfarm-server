package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import org.junit.jupiter.api.Test;

class GymLeaderTest {

  @Test
  void createsWithChallengeOrderInRange() {
    GymLeader leader = GymLeader.create("GYM001", "돌바위 관장", 1, "BDG101", null);

    assertEquals("GYM001", leader.getCode());
    assertEquals(1, leader.getChallengeOrder());
    assertEquals("BDG101", leader.getBadgeCode());
  }

  @Test
  void rejectsChallengeOrderOutOfRange() {
    assertThrows(
        IllegalArgumentException.class, () -> GymLeader.create("GYM000", "이름", 0, "BDG100", null));
    assertThrows(
        IllegalArgumentException.class, () -> GymLeader.create("GYM009", "이름", 9, "BDG109", null));
  }

  @Test
  void animalOrderNoIsBoundedByAnimalCount() {
    assertThrows(IllegalArgumentException.class, () -> gymLeaderAnimal(0));
    assertThrows(IllegalArgumentException.class, () -> gymLeaderAnimal(GymLeader.ANIMAL_COUNT + 1));

    GymLeaderAnimal animal = gymLeaderAnimal(GymLeader.ANIMAL_COUNT);

    assertEquals(GymLeader.ANIMAL_COUNT, animal.getOrderNo());
    assertEquals("두부", animal.getAnimalName());
  }

  private GymLeaderAnimal gymLeaderAnimal(int orderNo) {
    return GymLeaderAnimal.create(
        1L,
        orderNo,
        AnimalName.from("두부"),
        CardType.GROUND,
        Tier.C,
        CardSkill.GROUND_PAW_STRIKE,
        CardSkill.GROUND_LEAF_GUARD,
        null);
  }
}
