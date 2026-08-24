package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BattlePolicyConfigurationTest {

  @Autowired private BattlePolicy battlePolicy;

  @Test
  void bindsBattleDurationsFromConfiguration() {
    assertEquals(Duration.ofMinutes(30), battlePolicy.restDuration());
    assertEquals(Duration.ofMinutes(30), battlePolicy.abandonThreshold());
  }
}
