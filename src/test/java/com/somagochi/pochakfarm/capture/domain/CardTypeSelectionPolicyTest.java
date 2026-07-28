package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.random.RandomProvider;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CardTypeSelectionPolicyTest {

  @ParameterizedTest
  @MethodSource("cardTypeCases")
  void selectsEachCardTypeWithEqualProbability(int randomValue, CardType expected) {
    RandomProvider randomProvider = mock(RandomProvider.class);
    given(randomProvider.nextInt(CardType.values().length)).willReturn(randomValue);
    CardTypeSelectionPolicy policy = new CardTypeSelectionPolicy(randomProvider);

    assertEquals(expected, policy.select());
  }

  private static Stream<Arguments> cardTypeCases() {
    CardType[] types = CardType.values();
    return IntStream.range(0, types.length).mapToObj(index -> Arguments.of(index, types[index]));
  }
}
