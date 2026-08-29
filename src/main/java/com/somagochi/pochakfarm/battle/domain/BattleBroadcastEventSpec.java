package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import java.util.Objects;

public record BattleBroadcastEventSpec(
    BattleEventCode eventCode,
    BattleSide animalSide,
    CardSkill skill,
    BattleSide winnerSide,
    Integer points) {

  public BattleBroadcastEventSpec {
    validate(eventCode, animalSide, skill, winnerSide, points);
  }

  public static BattleBroadcastEventSpec animal(BattleEventCode eventCode, BattleSide animalSide) {
    return new BattleBroadcastEventSpec(
        eventCode, Objects.requireNonNull(animalSide), null, null, null);
  }

  public static BattleBroadcastEventSpec skill(
      BattleEventCode eventCode, BattleSide animalSide, CardSkill skill) {
    return new BattleBroadcastEventSpec(
        eventCode, Objects.requireNonNull(animalSide), Objects.requireNonNull(skill), null, null);
  }

  public static BattleBroadcastEventSpec point(BattleSide winnerSide, int points) {
    return new BattleBroadcastEventSpec(
        BattleEventCode.BATTLE_POINT_APPLIED,
        null,
        null,
        Objects.requireNonNull(winnerSide),
        points);
  }

  public static BattleBroadcastEventSpec simple(BattleEventCode eventCode) {
    return new BattleBroadcastEventSpec(eventCode, null, null, null, null);
  }

  static void validate(
      BattleEventCode eventCode,
      BattleSide animalSide,
      CardSkill skill,
      BattleSide winnerSide,
      Integer points) {
    Objects.requireNonNull(eventCode);
    boolean valid =
        switch (eventCode) {
          case TIER_ADVANTAGE, TYPE_ADVANTAGE ->
              animalSide != null && skill == null && winnerSide == null && points == null;
          case SKILL_NOT_SELECTED ->
              animalSide == BattleSide.USER
                  && skill == null
                  && winnerSide == null
                  && points == null;
          case SKILL_TRIGGERED, SKILL_FAILED ->
              animalSide != null && skill != null && winnerSide == null && points == null;
          case SKILL_OFFSET ->
              animalSide == null && skill == null && winnerSide == null && points == null;
          case BATTLE_POINT_APPLIED ->
              animalSide == null
                  && skill == null
                  && winnerSide != null
                  && points != null
                  && points > 0;
        };
    if (!valid) {
      throw new IllegalArgumentException("Invalid broadcast event payload: " + eventCode);
    }
  }
}
