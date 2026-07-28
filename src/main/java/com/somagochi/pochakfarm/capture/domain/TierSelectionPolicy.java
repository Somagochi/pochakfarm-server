package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.common.random.RandomProvider;
import org.springframework.stereotype.Component;

@Component
public class TierSelectionPolicy {

  private static final int TOTAL_WEIGHT = 10_000;
  private static final Tier[] TIER_ORDER = Tier.values();

  private static final int[] LEVEL_1_TO_5 = {7_000, 2_900, 100, 0, 0, 0};
  private static final int[] LEVEL_6_TO_10 = {5_800, 3_500, 650, 50, 0, 0};
  private static final int[] LEVEL_11_TO_20 = {4_490, 3_800, 1_400, 250, 50, 10};
  private static final int[] LEVEL_21_TO_30 = {3_170, 3_600, 2_200, 800, 180, 50};
  private static final int[] LEVEL_31_TO_40 = {2_100, 3_150, 2_700, 1_400, 500, 150};
  private static final int[] LEVEL_41_TO_45 = {1_400, 2_600, 3_000, 1_900, 800, 300};
  private static final int[] LEVEL_46_TO_50 = {1_000, 2_300, 3_000, 2_200, 1_000, 500};

  private final RandomProvider randomProvider;

  public TierSelectionPolicy(RandomProvider randomProvider) {
    this.randomProvider = randomProvider;
  }

  public Tier select(int level) {
    int value = randomProvider.nextInt(TOTAL_WEIGHT);
    int cumulativeWeight = 0;
    int[] weights = weightsFor(level);
    for (int index = 0; index < TIER_ORDER.length; index++) {
      cumulativeWeight += weights[index];
      if (value < cumulativeWeight) {
        return TIER_ORDER[index];
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
    if (level <= 45) {
      return LEVEL_41_TO_45;
    }
    return LEVEL_46_TO_50;
  }
}
