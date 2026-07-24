package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CardTypeSelectionPolicy {

  private static final List<CardType> TYPES =
      List.of(CardType.SKY, CardType.GROUND, CardType.SPACE, CardType.SEA);

  private final CaptureRandom random;

  public CardTypeSelectionPolicy(CaptureRandom random) {
    this.random = random;
  }

  public CardType select() {
    return TYPES.get(random.nextInt(TYPES.size()));
  }
}
