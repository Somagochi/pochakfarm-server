package com.somagochi.pochakfarm.characterization.domain;

public interface CharacterizerClient {

  CharacterizerResult characterize(
      String sourceImageBase64,
      String sourceImageContentType,
      String animalName,
      CardMetadata metadata);
}
