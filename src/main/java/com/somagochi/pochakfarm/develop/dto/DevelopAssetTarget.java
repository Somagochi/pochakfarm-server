package com.somagochi.pochakfarm.develop.dto;

public enum DevelopAssetTarget {
  ACHIEVEMENT("achievement"),
  BADGE("badge"),
  GYM_LEADER("gym-leader"),
  GYM_LEADER_THUMBNAIL("gym-leader-thumbnail"),
  GYM_LEADER_ANIMAL("gym-leader-animal");

  private final String purpose;

  DevelopAssetTarget(String purpose) {
    this.purpose = purpose;
  }

  public String purpose() {
    return purpose;
  }
}
