package com.somagochi.pochakfarm.capture.domain;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CaptureDifficultyPolicy {

  private static final Map<Tier, CaptureDifficulty> DIFFICULTIES =
      Map.of(
          Tier.C, new CaptureDifficulty(3_200),
          Tier.B, new CaptureDifficulty(2_800),
          Tier.A, new CaptureDifficulty(2_400),
          Tier.S, new CaptureDifficulty(2_000),
          Tier.SS, new CaptureDifficulty(1_700),
          Tier.SSS, new CaptureDifficulty(1_400));

  public CaptureDifficulty forTier(Tier tier) {
    return DIFFICULTIES.get(tier);
  }
}
