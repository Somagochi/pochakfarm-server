package com.somagochi.pochakfarm.battle.domain;

public enum GymLeaderType {
  GROUND("땅"),
  SKY("하늘"),
  SPACE("우주"),
  SEA("바다"),
  MIXED("복합");

  private final String label;

  GymLeaderType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public GymLeaderType counter() {
    return switch (this) {
      case GROUND -> SKY;
      case SKY -> SPACE;
      case SPACE -> SEA;
      case SEA -> GROUND;
      case MIXED -> MIXED;
    };
  }
}
