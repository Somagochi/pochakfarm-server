package com.somagochi.pochakfarm.characterization.domain;

import org.springframework.web.multipart.MultipartFile;

public interface CharacterizerClient {

  CharacterizerResult characterize(MultipartFile image, String animalName);
}
