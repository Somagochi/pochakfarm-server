package com.somagochi.pochakfarm.develop.dto;

import com.somagochi.pochakfarm.battle.domain.GymLeader;
import java.util.List;

public record DevelopGymLeaderAssetView(
    Long id,
    String code,
    String name,
    Integer challengeOrder,
    String thumbnailKey,
    String thumbnailUrl,
    String imageKey,
    String imageUrl,
    String badgeCode,
    String badgeName,
    String badgeImageUrl,
    List<DevelopGymLeaderAnimalView> animals) {

  public static DevelopGymLeaderAssetView of(
      GymLeader gymLeader,
      String thumbnailUrl,
      String imageUrl,
      String badgeName,
      String badgeImageUrl,
      List<DevelopGymLeaderAnimalView> animals) {
    return new DevelopGymLeaderAssetView(
        gymLeader.getId(),
        gymLeader.getCode(),
        gymLeader.getName(),
        gymLeader.getChallengeOrder(),
        gymLeader.getThumbnailKey(),
        thumbnailUrl,
        gymLeader.getImageKey(),
        imageUrl,
        gymLeader.getBadgeCode(),
        badgeName,
        badgeImageUrl,
        animals);
  }
}
