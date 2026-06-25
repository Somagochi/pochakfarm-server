package com.somagochi.pochakfarm.common.logging;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DiscordLogbackAppenderTest {

  @Test
  void buildsJsonPayloadWithLevelAndLogger() {
    String payload = DiscordLogbackAppender.buildPayload("com.example.Foo", "ERROR", "boom");

    assertTrue(payload.startsWith("{\"content\":\""));
    assertTrue(payload.contains("[ERROR] com.example.Foo"));
    assertTrue(payload.contains("boom"));
    assertTrue(payload.endsWith("\"}"));
  }

  @Test
  void escapesQuotesAndNewlines() {
    String payload = DiscordLogbackAppender.buildPayload("logger", "ERROR", "line1\n\"quoted\"");

    assertTrue(payload.contains("\\n"));
    assertTrue(payload.contains("\\\"quoted\\\""));
  }

  @Test
  void truncatesContentToDiscordLimit() {
    String longMessage = "x".repeat(5000);

    String payload = DiscordLogbackAppender.buildPayload("logger", "ERROR", longMessage);

    // content(이스케이프 전 원문)는 2000자 이하로 잘리고 말줄임표로 끝난다.
    assertTrue(payload.contains("..."));
    assertTrue(payload.length() < 2100);
  }
}
