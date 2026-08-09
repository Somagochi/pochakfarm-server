package com.somagochi.pochakfarm.coupon.application;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class CouponCodeGenerator {

  private final SecureRandom random = new SecureRandom();

  public String generate(String alphabet, int length) {
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    return builder.toString();
  }
}
