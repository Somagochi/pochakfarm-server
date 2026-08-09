package com.somagochi.pochakfarm.develop.dto;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import java.util.List;

public record DevelopAchievementAssetView(
    Long id,
    String code,
    String title,
    String description,
    AchievementCategory category,
    boolean hidden,
    long targetValue,
    String unachievedImageKey,
    String unachievedImageUrl,
    String achievedImageKey,
    String achievedImageUrl,
    List<DevelopAchievementRewardView> rewards) {

  public static DevelopAchievementAssetView of(
      Achievement achievement,
      String unachievedImageUrl,
      String achievedImageUrl,
      List<DevelopAchievementRewardView> rewards) {
    return new DevelopAchievementAssetView(
        achievement.getId(),
        achievement.getCode(),
        achievement.getTitle(),
        achievement.getDescription(),
        achievement.getCategory(),
        achievement.isHidden(),
        achievement.getTargetValue(),
        achievement.getUnachievedImageKey(),
        unachievedImageUrl,
        achievement.getAchievedImageKey(),
        achievedImageUrl,
        rewards);
  }
}
