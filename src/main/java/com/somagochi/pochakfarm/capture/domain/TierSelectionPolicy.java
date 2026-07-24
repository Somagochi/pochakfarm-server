package com.somagochi.pochakfarm.capture.domain;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TierSelectionPolicy {

  private static final int TOTAL_WEIGHT = 10_000;
  private static final List<Tier> TIER_ORDER =
      List.of(Tier.C, Tier.B, Tier.A, Tier.S, Tier.SS, Tier.SSS);

  private static final int[] LEVEL_1_TO_5 = {7_000, 2_900, 100, 0, 0, 0};
  private static final int[] LEVEL_6_TO_10 = {5_800, 3_500, 650, 50, 0, 0};
  private static final int[] LEVEL_11_TO_20 = {4_500, 3_800, 1_400, 250, 50, 0};
  private static final int[] LEVEL_21_TO_30 = {3_200, 3_600, 2_200, 800, 180, 20};
  private static final int[] LEVEL_31_TO_40 = {2_200, 3_200, 2_700, 1_400, 450, 50};
  private static final int[] LEVEL_41_TO_50 = {1_500, 2_700, 3_000, 1_800, 800, 200};

  private final CaptureRandom random;

  public TierSelectionPolicy(CaptureRandom random) {
    this.random = random;
  }

  public Tier select(int level) {
    int value = random.nextInt(TOTAL_WEIGHT);
    int cumulativeWeight = 0;
    int[] weights = weightsFor(level);
    for (int index = 0; index < TIER_ORDER.size(); index++) {
      cumulativeWeight += weights[index];
      if (value < cumulativeWeight) {
        return TIER_ORDER.get(index);
      }
    }
    throw new IllegalStateException("Tier weights must add up to " + TOTAL_WEIGHT);
  }

  private int[] weightsFor(int level) {
    if (level <= 5) {
      return LEVEL_1_TO_5;
    }
    if (level <= 10) {
      return LEVEL_6_TO_10;
    }
    if (level <= 20) {
      return LEVEL_11_TO_20;
    }
    if (level <= 30) {
      return LEVEL_21_TO_30;
    }
    if (level <= 40) {
      return LEVEL_31_TO_40;
    }
    return LEVEL_41_TO_50;
  }
}
