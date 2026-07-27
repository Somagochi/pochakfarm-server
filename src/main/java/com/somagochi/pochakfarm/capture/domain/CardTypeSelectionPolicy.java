package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.random.RandomProvider;
import org.springframework.stereotype.Component;

@Component
public class CardTypeSelectionPolicy {

  private static final CardType[] TYPES = CardType.values();

  private final RandomProvider randomProvider;

  public CardTypeSelectionPolicy(RandomProvider randomProvider) {
    this.randomProvider = randomProvider;
  }

  public CardType select() {
    return TYPES[randomProvider.nextInt(TYPES.length)];
  }
}
