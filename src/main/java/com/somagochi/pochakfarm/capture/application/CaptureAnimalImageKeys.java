package com.somagochi.pochakfarm.capture.application;

final class CaptureAnimalImageKeys {

  private CaptureAnimalImageKeys() {}

  static String of(Long userId, Long captureId) {
    return "public/capture-animal/%d/%d.png".formatted(userId, captureId);
  }
}
