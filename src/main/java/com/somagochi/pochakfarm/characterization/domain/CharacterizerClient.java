package com.somagochi.pochakfarm.characterization.domain;

public interface CharacterizerClient {

  CharacterizerResult characterize(String sourceImageUrl, String animalName, CardMetadata metadata);
}
