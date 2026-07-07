package com.somagochi.pochakfarm.device.application;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class DeviceTokenGenerator {

  private static final String PREFIX = "dev_";
  private static final int TOKEN_BYTES = 24;

  private final SecureRandom random = new SecureRandom();

  public String generate() {
    byte[] bytes = new byte[TOKEN_BYTES];
    random.nextBytes(bytes);
    return PREFIX + HexFormat.of().formatHex(bytes);
  }
}
