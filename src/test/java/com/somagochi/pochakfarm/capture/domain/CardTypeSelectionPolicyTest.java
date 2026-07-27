package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.stream.IntStream;
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
    CardType[] types = CardType.values();
    return IntStream.range(0, types.length).mapToObj(index -> Arguments.of(index, types[index]));
  }
}
