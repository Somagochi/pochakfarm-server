package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CardTypeSelectionPolicyTest {

  @ParameterizedTest
  @MethodSource("cardTypeCases")
  void selectsEachCardTypeWithEqualProbability(int randomValue, CardType expected) {
    CardTypeSelectionPolicy policy = new CardTypeSelectionPolicy(bound -> randomValue);

    assertEquals(expected, policy.select());
  }

  private static Stream<Arguments> cardTypeCases() {
    return Stream.of(
        Arguments.of(0, CardType.SKY),
        Arguments.of(1, CardType.GROUND),
        Arguments.of(2, CardType.SPACE),
        Arguments.of(3, CardType.SEA));
  }
}
