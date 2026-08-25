package com.somagochi.pochakfarm.common.random;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class RandomProvider implements RandomSource {

  @Override
  public int nextInt(int bound) {
    return ThreadLocalRandom.current().nextInt(bound);
  }
}
