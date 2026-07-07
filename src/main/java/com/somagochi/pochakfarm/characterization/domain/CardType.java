package com.somagochi.pochakfarm.characterization.domain;

public enum CardType {
  GROUND("땅"),
  SKY("하늘"),
  SPACE("우주"),
  SEA("바다");

  private final String label;

  CardType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
