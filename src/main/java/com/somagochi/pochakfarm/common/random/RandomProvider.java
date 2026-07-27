package com.somagochi.pochakfarm.common.random;

@FunctionalInterface
public interface RandomProvider {

  int nextInt(int bound);
}
