package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TierSelectionPolicyTest {

  @ParameterizedTest
  @MethodSource("tierCases")
  void selectsTierFromLevelAndWeightedRandomValue(int level, int randomValue, Tier expected) {
    TierSelectionPolicy policy = new TierSelectionPolicy(bound -> randomValue);

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
        Arguments.of(11, 4_499, Tier.C),
        Arguments.of(20, 4_500, Tier.B),
        Arguments.of(11, 8_300, Tier.A),
        Arguments.of(20, 9_700, Tier.S),
        Arguments.of(11, 9_950, Tier.SS),
        Arguments.of(21, 3_199, Tier.C),
        Arguments.of(30, 3_200, Tier.B),
        Arguments.of(21, 6_800, Tier.A),
        Arguments.of(30, 9_000, Tier.S),
        Arguments.of(21, 9_800, Tier.SS),
        Arguments.of(30, 9_980, Tier.SSS),
        Arguments.of(31, 2_199, Tier.C),
        Arguments.of(40, 2_200, Tier.B),
        Arguments.of(31, 5_400, Tier.A),
        Arguments.of(40, 8_100, Tier.S),
        Arguments.of(31, 9_500, Tier.SS),
        Arguments.of(40, 9_950, Tier.SSS),
        Arguments.of(41, 1_499, Tier.C),
        Arguments.of(50, 1_500, Tier.B),
        Arguments.of(41, 4_200, Tier.A),
        Arguments.of(50, 7_200, Tier.S),
        Arguments.of(41, 9_000, Tier.SS),
        Arguments.of(50, 9_800, Tier.SSS),
        Arguments.of(51, 9_800, Tier.SSS));
  }
}
