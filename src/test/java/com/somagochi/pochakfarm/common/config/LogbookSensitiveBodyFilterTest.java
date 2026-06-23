package com.somagochi.pochakfarm.common.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.zalando.logbook.BodyFilter;

class LogbookSensitiveBodyFilterTest {

  private final BodyFilter bodyFilter = new LogbookConfig().sensitiveBodyFilter();

  @Test
  void masksTokenFieldsInJsonBody() {
    String body =
        "{\"accessToken\":\"aaa.bbb.ccc\",\"refreshToken\":\"rrr.sss.ttt\",\"token\":\"social-xyz\"}";

    String filtered = bodyFilter.filter("application/json", body);

    assertFalse(filtered.contains("aaa.bbb.ccc"));
    assertFalse(filtered.contains("rrr.sss.ttt"));
    assertFalse(filtered.contains("social-xyz"));
    assertTrue(filtered.contains("***"));
  }

  @Test
  void keepsNonSensitiveFields() {
    String body = "{\"provider\":\"kakao\",\"accessToken\":\"aaa.bbb.ccc\"}";

    String filtered = bodyFilter.filter("application/json", body);

    assertTrue(filtered.contains("kakao"));
    assertFalse(filtered.contains("aaa.bbb.ccc"));
  }
}
