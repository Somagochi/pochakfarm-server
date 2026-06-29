package com.somagochi.pochakfarm.characterization.domain;

public record CardMetadata(
    CardType cardType, int power, CardSkill skill1, CardSkill skill2, String cardNo) {

  public String cardTypeLabel() {
    return cardType.label();
  }

  public CardMetadata withCardNo(String cardNo) {
    return new CardMetadata(cardType, power, skill1, skill2, cardNo);
  }
}
