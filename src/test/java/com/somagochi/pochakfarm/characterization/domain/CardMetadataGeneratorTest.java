package com.somagochi.pochakfarm.characterization.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CardMetadataGeneratorTest {

  @Test
  void eachCardTypeHasThirtySkills() {
    for (CardType cardType : CardType.values()) {
      assertEquals(30, CardSkill.forType(cardType).size());
    }
  }

  @Test
  void eachCardTypeHasTenSkillsForEveryBattleType() {
    for (CardType cardType : CardType.values()) {
      for (SkillBattleType battleType : SkillBattleType.values()) {
        assertEquals(10, CardSkill.forType(cardType, battleType).size());
      }
    }
  }

  @Test
  void generatesMetadataWithinCardPolicy() {
    CardMetadataGenerator generator = new CardMetadataGenerator(new Random(0));

    CardMetadata metadata = generator.generate();

    assertTrue(metadata.power() >= 70 && metadata.power() <= 90);
    assertEquals(metadata.cardType(), metadata.skill1().cardType());
    assertEquals(metadata.cardType(), metadata.skill2().cardType());
    assertNotEquals(metadata.skill1(), metadata.skill2());
    assertNotEquals(metadata.skill1().battleType(), metadata.skill2().battleType());
    assertEquals("001", metadata.cardNo());
  }

  @Test
  void alwaysGeneratesSkillsWithDifferentBattleTypes() {
    CardMetadataGenerator generator = new CardMetadataGenerator(new Random(0));

    for (CardType cardType : CardType.values()) {
      for (int i = 0; i < 100; i++) {
        CardMetadata metadata = generator.generate(cardType);

        assertNotEquals(metadata.skill1().battleType(), metadata.skill2().battleType());
      }
    }
  }

  @Test
  void canGenerateEveryCardType() {
    CardMetadataGenerator generator = new CardMetadataGenerator(new Random(0));
    Set<CardType> generatedTypes = EnumSet.noneOf(CardType.class);

    for (int i = 0; i < 100; i++) {
      generatedTypes.add(generator.generate().cardType());
    }

    assertEquals(EnumSet.allOf(CardType.class), generatedTypes);
  }
}
