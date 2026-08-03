package com.somagochi.pochakfarm.develop.dto;

public record DevelopAchievementAssetPresignRequest(String contentType, String target) {

  public boolean isBadgeTarget() {
    return "badge".equals(target);
  }
}
