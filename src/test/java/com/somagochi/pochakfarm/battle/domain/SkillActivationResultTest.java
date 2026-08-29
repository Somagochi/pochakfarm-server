package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import org.junit.jupiter.api.Test;

class SkillActivationResultTest {

  private static final CardSkill SKILL = CardSkill.SEA_SEASHELL_SHIELD;

  @Test
  void acceptsPositivePointsWhenSkillActivated() {
    assertDoesNotThrow(() -> SkillActivationResult.selected(SKILL, true, 1));
  }

  @Test
  void rejectsZeroPointsWhenSkillActivated() {
    assertThrows(
        IllegalArgumentException.class, () -> SkillActivationResult.selected(SKILL, true, 0));
  }

  @Test
  void rejectsPointsWhenSkillFailed() {
    assertThrows(
        IllegalArgumentException.class, () -> SkillActivationResult.selected(SKILL, false, 1));
  }
}
