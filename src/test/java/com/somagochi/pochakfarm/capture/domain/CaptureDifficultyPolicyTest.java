package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CaptureDifficultyPolicyTest {

  private final CaptureDifficultyPolicy policy = new CaptureDifficultyPolicy();

  @ParameterizedTest
  @MethodSource("difficultyCases")
  void returnsRingShrinkDurationForTier(Tier tier, int ringShrinkDurationMs) {
    CaptureDifficulty difficulty = policy.forTier(tier);

    assertEquals(ringShrinkDurationMs, difficulty.ringShrinkDurationMs());
  }

  private static Stream<Arguments> difficultyCases() {
    return Stream.of(
        Arguments.of(Tier.C, 3_200),
        Arguments.of(Tier.B, 2_800),
        Arguments.of(Tier.A, 2_400),
        Arguments.of(Tier.S, 2_000),
        Arguments.of(Tier.SS, 1_700),
        Arguments.of(Tier.SSS, 1_400));
  }
}
