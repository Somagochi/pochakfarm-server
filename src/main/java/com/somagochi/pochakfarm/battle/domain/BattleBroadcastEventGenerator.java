package com.somagochi.pochakfarm.battle.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class BattleBroadcastEventGenerator {

  public List<BattleBroadcastEventSpec> advantage(
      BattleEventCode eventCode, BattlePositionChange change) {
    Objects.requireNonNull(eventCode);
    Objects.requireNonNull(change);
    if (eventCode != BattleEventCode.TIER_ADVANTAGE
        && eventCode != BattleEventCode.TYPE_ADVANTAGE) {
      throw new IllegalArgumentException("Unsupported advantage event code: " + eventCode);
    }
    if (change.calculatedPoints() == 0) {
      return List.of();
    }

    BattleSide advantagedSide = sideOf(change.calculatedPoints());
    List<BattleBroadcastEventSpec> events = new ArrayList<>();
    events.add(BattleBroadcastEventSpec.animal(eventCode, advantagedSide));
    addPointEvent(events, change.appliedPoints());
    return List.copyOf(events);
  }

  public List<BattleBroadcastEventSpec> skill(SkillBattleResolution resolution) {
    Objects.requireNonNull(resolution);
    List<BattleBroadcastEventSpec> events = new ArrayList<>();
    events.add(activation(BattleSide.USER, resolution.user()));
    events.add(activation(BattleSide.NPC, resolution.npc()));
    if (isOffset(resolution)) {
      events.add(BattleBroadcastEventSpec.simple(BattleEventCode.SKILL_OFFSET));
    }
    addPointEvent(events, resolution.positionChange().appliedPoints());
    return List.copyOf(events);
  }

  private BattleBroadcastEventSpec activation(BattleSide side, SkillActivationResult activation) {
    return switch (activation.status()) {
      case NOT_SELECTED ->
          BattleBroadcastEventSpec.animal(BattleEventCode.SKILL_NOT_SELECTED, side);
      case ACTIVATED ->
          BattleBroadcastEventSpec.skill(
              BattleEventCode.SKILL_TRIGGERED, side, activation.skill().orElseThrow());
      case FAILED ->
          BattleBroadcastEventSpec.skill(
              BattleEventCode.SKILL_FAILED, side, activation.skill().orElseThrow());
    };
  }

  private boolean isOffset(SkillBattleResolution resolution) {
    return resolution.user().status() == SkillActivationStatus.ACTIVATED
        && resolution.npc().status() == SkillActivationStatus.ACTIVATED
        && resolution.user().points() == resolution.npc().points();
  }

  private void addPointEvent(List<BattleBroadcastEventSpec> events, int appliedPoints) {
    if (appliedPoints != 0) {
      events.add(BattleBroadcastEventSpec.point(sideOf(appliedPoints), Math.abs(appliedPoints)));
    }
  }

  private BattleSide sideOf(int signedPoints) {
    return signedPoints > 0 ? BattleSide.USER : BattleSide.NPC;
  }
}
