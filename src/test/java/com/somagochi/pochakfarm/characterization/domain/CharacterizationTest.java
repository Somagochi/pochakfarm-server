package com.somagochi.pochakfarm.characterization.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CharacterizationTest {

  @Test
  void doesNotKeepOriginalImageKey() {
    boolean hasOriginalImageKey =
        Arrays.stream(Characterization.class.getDeclaredFields())
            .anyMatch(field -> field.getName().equals("originalImageKey"));

    assertFalse(hasOriginalImageKey);
  }
}
