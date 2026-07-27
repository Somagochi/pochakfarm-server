package com.somagochi.pochakfarm.common.random;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class ThreadLocalRandomProvider implements RandomProvider {

  @Override
  public int nextInt(int bound) {
    return ThreadLocalRandom.current().nextInt(bound);
  }
}
