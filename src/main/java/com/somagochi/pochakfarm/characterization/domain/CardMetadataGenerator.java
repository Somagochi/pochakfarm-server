package com.somagochi.pochakfarm.characterization.domain;

import java.security.SecureRandom;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

@Component
public class CardMetadataGenerator {

  private static final String FIXED_CARD_NO = "001";

  private final RandomGenerator random;

  public CardMetadataGenerator() {
    this(new SecureRandom());
  }

  CardMetadataGenerator(RandomGenerator random) {
    this.random = random;
  }

  public CardMetadata generate() {
    CardType cardType = pickCardType();
    List<CardSkill> skills = CardSkill.forType(cardType);
    int firstIndex = random.nextInt(skills.size());
    int secondIndex = random.nextInt(skills.size() - 1);
    if (secondIndex >= firstIndex) {
      secondIndex++;
    }
    return new CardMetadata(
        cardType, pickPower(), skills.get(firstIndex), skills.get(secondIndex), FIXED_CARD_NO);
  }

  private CardType pickCardType() {
    return CardType.SKY;
  }

  private int pickPower() {
    int bucket = random.nextInt(100);
    if (bucket < 70) {
      return random.nextInt(11) + 70;
    }
    if (bucket < 95) {
      return random.nextInt(5) + 81;
    }
    return random.nextInt(5) + 86;
  }
}
