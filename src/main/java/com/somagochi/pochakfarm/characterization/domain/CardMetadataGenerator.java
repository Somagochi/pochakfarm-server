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
    CardType[] cardTypes = CardType.values();
    return cardTypes[random.nextInt(cardTypes.length)];
  }

  private int pickPower() {
    int chance = randomPercentage();
    if (chance < 70) {
      return randomPowerBetween(70, 80);
    }
    if (chance < 95) {
      return randomPowerBetween(81, 85);
    }
    return randomPowerBetween(86, 90);
  }

  private int randomPercentage() {
    return random.nextInt(100);
  }

  private int randomPowerBetween(int minInclusive, int maxInclusive) {
    return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
  }
}
