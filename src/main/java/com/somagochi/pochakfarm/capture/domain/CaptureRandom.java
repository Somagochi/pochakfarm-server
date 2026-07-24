package com.somagochi.pochakfarm.capture.domain;

@FunctionalInterface
public interface CaptureRandom {

  int nextInt(int bound);
}
