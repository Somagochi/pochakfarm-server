package com.somagochi.pochakfarm.capture.infrastructure;

import com.somagochi.pochakfarm.capture.domain.CaptureRandom;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class ThreadLocalCaptureRandom implements CaptureRandom {

  @Override
  public int nextInt(int bound) {
    return ThreadLocalRandom.current().nextInt(bound);
  }
}
