package com.somagochi.pochakfarm.device.presentation;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class DeviceTokenCookieFactory {

  public static final String COOKIE_NAME = "deviceToken";
  private static final Duration MAX_AGE = Duration.ofDays(365);

  public ResponseCookie create(String deviceToken) {
    return ResponseCookie.from(COOKIE_NAME, deviceToken)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(MAX_AGE)
        .build();
  }
}
