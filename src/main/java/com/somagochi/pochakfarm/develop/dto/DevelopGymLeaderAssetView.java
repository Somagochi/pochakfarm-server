package com.somagochi.pochakfarm.develop.dto;

import com.somagochi.pochakfarm.battle.domain.GymLeader;
import java.util.List;

public record DevelopGymLeaderAssetView(
    Long id,
    String code,
    String name,
    Integer challengeOrder,
    String imageKey,
    String imageUrl,
    String badgeCode,
    String badgeName,
    String badgeImageUrl,
    List<DevelopGymLeaderAnimalView> animals) {

  public static DevelopGymLeaderAssetView of(
      GymLeader gymLeader,
      String imageUrl,
      String badgeName,
      String badgeImageUrl,
      List<DevelopGymLeaderAnimalView> animals) {
    return new DevelopGymLeaderAssetView(
        gymLeader.getId(),
        gymLeader.getCode(),
        gymLeader.getName(),
        gymLeader.getChallengeOrder(),
        gymLeader.getImageKey(),
        imageUrl,
        gymLeader.getBadgeCode(),
        badgeName,
        badgeImageUrl,
        animals);
  }
}
