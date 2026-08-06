package com.somagochi.pochakfarm.achievement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AchievementResponse(
    Long id,
    String code,
    String title,
    String description,
    AchievementCategory category,
    boolean hidden,
    String imageUrl,
    Progress progress,
    boolean achieved,
    AchievedInfo achievedInfo,
    List<AchievementRewardResponse> rewards) {

  public static AchievementResponse of(
      Achievement achievement,
      long current,
      UserAchievement record,
      List<AchievementRewardResponse> rewards,
      String unachievedImageUrl,
      String achievedImageUrl) {
    if (record == null) {
      return unachievedResponse(achievement, current, rewards, unachievedImageUrl);
    }
    return achievedResponse(achievement, current, record, rewards, achievedImageUrl);
  }

  private static AchievementResponse achievedResponse(
      Achievement achievement,
      long current,
      UserAchievement record,
      List<AchievementRewardResponse> rewards,
      String achievedImageUrl) {
    return new AchievementResponse(
        achievement.getId(),
        achievement.getCode(),
        achievement.getTitle(),
        achievement.getDescription(),
        achievement.getCategory(),
        achievement.isHidden(),
        achievedImageUrl,
        Progress.achieved(current, achievement.getTargetValue()),
        true,
        new AchievedInfo(record.getAchievedAt(), record.isRewardClaimed()),
        rewards);
  }

  private static AchievementResponse unachievedResponse(
      Achievement achievement,
      long current,
      List<AchievementRewardResponse> rewards,
      String unachievedImageUrl) {
    if (achievement.isHidden()) {
      return lockedResponse(achievement);
    }
    return new AchievementResponse(
        achievement.getId(),
        achievement.getCode(),
        achievement.getTitle(),
        achievement.getDescription(),
        achievement.getCategory(),
        false,
        unachievedImageUrl,
        Progress.unachieved(current, achievement.getTargetValue()),
        false,
        null,
        rewards);
  }

  private static AchievementResponse lockedResponse(Achievement achievement) {
    return new AchievementResponse(
        achievement.getId(),
        achievement.getCode(),
        achievement.getTitle(),
        null,
        achievement.getCategory(),
        true,
        null,
        null,
        false,
        null,
        null);
  }

  public record Progress(long current, long target) {

    public static Progress unachieved(long current, long target) {
      return new Progress(current, target);
    }

    public static Progress achieved(long current, long target) {
      return new Progress(Math.min(current, target), target);
    }
  }

  public record AchievedInfo(Instant achievedAt, boolean rewardClaimed) {}
}
