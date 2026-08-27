package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "battle_actions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_battle_actions_battle_id_action_seq",
            columnNames = {"battle_id", "action_seq"}))
public class BattleAction extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "battle_id", nullable = false, updatable = false)
  private Long battleId;

  @Column(name = "action_seq", nullable = false, updatable = false)
  private Integer actionSeq;

  @Column(name = "entry_order", nullable = false, updatable = false)
  private Integer entryOrder;

  @Column(name = "action_no_in_entry", nullable = false, updatable = false)
  private Integer actionNoInEntry;

  @Enumerated(EnumType.STRING)
  @Column(name = "user_skill", updatable = false)
  private CardSkill userSkill;

  @Column(name = "user_skill_triggered", nullable = false, updatable = false)
  private Boolean userSkillTriggered;

  @Column(name = "user_move_distance", nullable = false, updatable = false)
  private Integer userMoveDistance;

  @Enumerated(EnumType.STRING)
  @Column(name = "npc_skill", nullable = false, updatable = false)
  private CardSkill npcSkill;

  @Column(name = "npc_skill_triggered", nullable = false, updatable = false)
  private Boolean npcSkillTriggered;

  @Column(name = "npc_move_distance", nullable = false, updatable = false)
  private Integer npcMoveDistance;

  @Column(name = "net_move_distance", nullable = false, updatable = false)
  private Integer netMoveDistance;

  @Column(name = "bar_position_before", nullable = false, updatable = false)
  private Integer barPositionBefore;

  @Column(name = "bar_position_after", nullable = false, updatable = false)
  private Integer barPositionAfter;

  private BattleAction(
      Long battleId,
      Integer actionSeq,
      CardSkill userSkill,
      boolean userSkillTriggered,
      int userMoveDistance,
      CardSkill npcSkill,
      boolean npcSkillTriggered,
      int npcMoveDistance,
      int netMoveDistance,
      int barPositionBefore,
      int barPositionAfter) {
    this.battleId = Objects.requireNonNull(battleId);
    this.actionSeq = validateActionSeq(actionSeq);
    this.entryOrder = entryOrderOf(this.actionSeq);
    this.actionNoInEntry = actionNoInEntryOf(this.actionSeq);
    this.userSkill = validateUserSelection(userSkill, userSkillTriggered, userMoveDistance);
    this.userSkillTriggered = userSkillTriggered;
    this.userMoveDistance = userMoveDistance;
    this.npcSkill = Objects.requireNonNull(npcSkill);
    this.npcSkillTriggered = npcSkillTriggered;
    this.npcMoveDistance = npcMoveDistance;
    this.netMoveDistance = netMoveDistance;
    this.barPositionBefore = barPositionBefore;
    this.barPositionAfter = barPositionAfter;
  }

  public static BattleAction record(
      Long battleId,
      Integer actionSeq,
      CardSkill userSkill,
      boolean userSkillTriggered,
      int userMoveDistance,
      CardSkill npcSkill,
      boolean npcSkillTriggered,
      int npcMoveDistance,
      int netMoveDistance,
      int barPositionBefore,
      int barPositionAfter) {
    return new BattleAction(
        battleId,
        actionSeq,
        userSkill,
        userSkillTriggered,
        userMoveDistance,
        npcSkill,
        npcSkillTriggered,
        npcMoveDistance,
        netMoveDistance,
        barPositionBefore,
        barPositionAfter);
  }

  public static BattleAction from(
      Long battleId, Integer actionSeq, SkillBattleResolution resolution) {
    Objects.requireNonNull(resolution);
    return new BattleAction(
        battleId,
        actionSeq,
        resolution.user().skill().orElse(null),
        resolution.user().status() == SkillActivationStatus.ACTIVATED,
        resolution.user().points(),
        resolution.npc().skill().orElseThrow(),
        resolution.npc().status() == SkillActivationStatus.ACTIVATED,
        resolution.npc().points(),
        resolution.netPoints(),
        resolution.positionChange().before().value(),
        resolution.positionChange().after().value());
  }

  public SkillActivationResult userActivation() {
    if (userSkill == null) {
      return SkillActivationResult.notSelected();
    }
    return SkillActivationResult.selected(userSkill, userSkillTriggered, userMoveDistance);
  }

  public SkillActivationResult npcActivation() {
    return SkillActivationResult.selected(npcSkill, npcSkillTriggered, npcMoveDistance);
  }

  public static int entryOrderOf(int actionSeq) {
    return (actionSeq - 1) / BattlePolicy.ACTIONS_PER_ENTRY + 1;
  }

  public static int actionNoInEntryOf(int actionSeq) {
    return (actionSeq - 1) % BattlePolicy.ACTIONS_PER_ENTRY + 1;
  }

  private static CardSkill validateUserSelection(
      CardSkill userSkill, boolean userSkillTriggered, int userMoveDistance) {
    if (userSkill == null && (userSkillTriggered || userMoveDistance != 0)) {
      throw new IllegalArgumentException("Not selected user skill cannot have an outcome");
    }
    return userSkill;
  }

  private static Integer validateActionSeq(Integer actionSeq) {
    Objects.requireNonNull(actionSeq);
    if (actionSeq < 1 || actionSeq > BattlePolicy.TOTAL_ACTION_COUNT) {
      throw new IllegalArgumentException("Action seq is out of range: " + actionSeq);
    }
    return actionSeq;
  }
}
