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
  void returnsInitialDifficultyForTier(Tier tier, int ringShrinkDurationMs, int successWindowMs) {
    CaptureDifficulty difficulty = policy.forTier(tier);

    assertEquals(10_000, difficulty.roundDurationMs());
    assertEquals(3, difficulty.maxThrows());
    assertEquals(ringShrinkDurationMs, difficulty.ringShrinkDurationMs());
    assertEquals(successWindowMs, difficulty.successWindowMs());
  }

  private static Stream<Arguments> difficultyCases() {
    return Stream.of(
        Arguments.of(Tier.C, 3_200, 320),
        Arguments.of(Tier.B, 2_800, 280),
        Arguments.of(Tier.A, 2_400, 240),
        Arguments.of(Tier.S, 2_000, 200),
        Arguments.of(Tier.SS, 1_700, 170),
        Arguments.of(Tier.SSS, 1_400, 140));
  }
}
