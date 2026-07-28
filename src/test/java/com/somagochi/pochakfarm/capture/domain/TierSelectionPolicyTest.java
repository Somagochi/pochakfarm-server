package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.common.random.RandomProvider;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TierSelectionPolicyTest {

  @ParameterizedTest
  @MethodSource("tierCases")
  void selectsTierFromLevelAndWeightedRandomValue(int level, int randomValue, Tier expected) {
    RandomProvider randomProvider = mock(RandomProvider.class);
    given(randomProvider.nextInt(10_000)).willReturn(randomValue);
    TierSelectionPolicy policy = new TierSelectionPolicy(randomProvider);

    assertEquals(expected, policy.select(level));
  }

  private static Stream<Arguments> tierCases() {
    return Stream.of(
        Arguments.of(1, 0, Tier.C),
        Arguments.of(5, 6_999, Tier.C),
        Arguments.of(1, 7_000, Tier.B),
        Arguments.of(5, 9_899, Tier.B),
        Arguments.of(1, 9_900, Tier.A),
        Arguments.of(5, 9_999, Tier.A),
        Arguments.of(6, 5_799, Tier.C),
        Arguments.of(10, 5_800, Tier.B),
        Arguments.of(6, 9_299, Tier.B),
        Arguments.of(6, 9_300, Tier.A),
        Arguments.of(10, 9_950, Tier.S),
        Arguments.of(11, 4_489, Tier.C),
        Arguments.of(20, 4_490, Tier.B),
        Arguments.of(11, 8_300, Tier.A),
        Arguments.of(20, 9_700, Tier.S),
        Arguments.of(11, 9_950, Tier.SS),
        Arguments.of(11, 9_990, Tier.SSS),
        Arguments.of(21, 3_169, Tier.C),
        Arguments.of(30, 3_170, Tier.B),
        Arguments.of(21, 6_800, Tier.A),
        Arguments.of(30, 9_000, Tier.S),
        Arguments.of(21, 9_800, Tier.SS),
        Arguments.of(30, 9_980, Tier.SSS),
        Arguments.of(31, 2_099, Tier.C),
        Arguments.of(40, 2_100, Tier.B),
        Arguments.of(31, 5_400, Tier.A),
        Arguments.of(40, 8_100, Tier.S),
        Arguments.of(31, 9_500, Tier.SS),
        Arguments.of(40, 9_950, Tier.SSS),
        Arguments.of(41, 1_399, Tier.C),
        Arguments.of(45, 1_400, Tier.B),
        Arguments.of(41, 4_200, Tier.A),
        Arguments.of(45, 7_200, Tier.S),
        Arguments.of(41, 9_000, Tier.SS),
        Arguments.of(45, 9_800, Tier.SSS),
        Arguments.of(46, 999, Tier.C),
        Arguments.of(50, 1_000, Tier.B),
        Arguments.of(46, 3_300, Tier.A),
        Arguments.of(50, 6_300, Tier.S),
        Arguments.of(46, 8_500, Tier.SS),
        Arguments.of(50, 9_500, Tier.SSS),
        Arguments.of(51, 9_800, Tier.SSS));
  }
}
