package com.somagochi.pochakfarm.capture.domain;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CaptureDifficultyPolicy {

  private static final int ROUND_DURATION_MS = 10_000;
  private static final int MAX_THROWS = 3;
  private static final Map<Tier, CaptureDifficulty> DIFFICULTIES =
      Map.of(
          Tier.C, difficulty(3_200, 320),
          Tier.B, difficulty(2_800, 280),
          Tier.A, difficulty(2_400, 240),
          Tier.S, difficulty(2_000, 200),
          Tier.SS, difficulty(1_700, 170),
          Tier.SSS, difficulty(1_400, 140));

  public CaptureDifficulty forTier(Tier tier) {
    return DIFFICULTIES.get(tier);
  }

  private static CaptureDifficulty difficulty(int ringShrinkDurationMs, int successWindowMs) {
    return new CaptureDifficulty(
        ROUND_DURATION_MS, MAX_THROWS, ringShrinkDurationMs, successWindowMs);
  }
}
