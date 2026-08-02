package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CaptureExperiencePolicyTest {

  private final CaptureExperiencePolicy policy = new CaptureExperiencePolicy();

  @ParameterizedTest
  @MethodSource("experienceCases")
  void returnsExperienceByTierAndResult(Tier tier, long success, long failure) {
    assertEquals(success, policy.experienceFor(tier, GameStatus.SUCCEEDED));
    assertEquals(failure, policy.experienceFor(tier, GameStatus.FAILED));
    assertEquals(0, policy.experienceFor(tier, GameStatus.EXPIRED));
  }

  private static Stream<Arguments> experienceCases() {
    return Stream.of(
        Arguments.of(Tier.C, 10L, 2L),
        Arguments.of(Tier.B, 15L, 3L),
        Arguments.of(Tier.A, 25L, 5L),
        Arguments.of(Tier.S, 45L, 9L),
        Arguments.of(Tier.SS, 80L, 16L),
        Arguments.of(Tier.SSS, 150L, 30L));
  }
}
