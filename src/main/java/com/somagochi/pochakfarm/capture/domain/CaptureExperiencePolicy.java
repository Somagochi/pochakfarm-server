package com.somagochi.pochakfarm.capture.domain;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CaptureExperiencePolicy {

  private static final Map<Tier, Long> SUCCESS_EXPERIENCE =
      Map.of(
          Tier.C, 10L,
          Tier.B, 15L,
          Tier.A, 25L,
          Tier.S, 45L,
          Tier.SS, 80L,
          Tier.SSS, 150L);
  private static final Map<Tier, Long> FAILURE_EXPERIENCE =
      Map.of(
          Tier.C, 2L,
          Tier.B, 3L,
          Tier.A, 5L,
          Tier.S, 9L,
          Tier.SS, 16L,
          Tier.SSS, 30L);

  public long experienceFor(Tier tier, GameStatus gameStatus) {
    return switch (gameStatus) {
      case SUCCEEDED -> SUCCESS_EXPERIENCE.get(tier);
      case FAILED -> FAILURE_EXPERIENCE.get(tier);
      case EXPIRED -> 0L;
      case PENDING -> throw new IllegalArgumentException("Pending game has no reward");
    };
  }
}
