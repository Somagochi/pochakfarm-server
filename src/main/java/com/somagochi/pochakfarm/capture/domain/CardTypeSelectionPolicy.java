package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import org.springframework.stereotype.Component;

@Component
public class CardTypeSelectionPolicy {

  private static final CardType[] TYPES = CardType.values();

  private final CaptureRandom random;

  public CardTypeSelectionPolicy(CaptureRandom random) {
    this.random = random;
  }

  public CardType select() {
    return TYPES[random.nextInt(TYPES.length)];
  }
}
