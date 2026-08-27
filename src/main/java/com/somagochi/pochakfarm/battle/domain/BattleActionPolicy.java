package com.somagochi.pochakfarm.battle.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class BattleActionPolicy {

  public static final Duration SKILL_SELECTION_TIME_LIMIT = Duration.ofSeconds(3);

  public Instant selectionExpiresAt(Instant openedAt) {
    return Objects.requireNonNull(openedAt).plus(SKILL_SELECTION_TIME_LIMIT);
  }

  public boolean isSelectionClosed(Instant openedAt, Instant now) {
    return !Objects.requireNonNull(now).isBefore(selectionExpiresAt(openedAt));
  }
}
