package com.somagochi.pochakfarm.coupon.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CouponCodeGeneratorTest {

  private final CouponCodeGenerator generator = new CouponCodeGenerator();

  @Test
  void generatesCodeOfRequestedLengthFromAlphabet() {
    String alphabet = "ABC123";

    for (int i = 0; i < 100; i++) {
      String code = generator.generate(alphabet, 6);

      assertEquals(6, code.length());
      for (char c : code.toCharArray()) {
        assertTrue(alphabet.indexOf(c) >= 0);
      }
    }
  }
}
