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
    name = "battle_broadcast_events",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_battle_broadcast_events_battle_id_event_seq",
            columnNames = {"battle_id", "event_seq"}))
public class BattleBroadcastEvent extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "battle_id", nullable = false, updatable = false)
  private Long battleId;

  @Column(name = "event_seq", nullable = false, updatable = false)
  private Integer eventSeq;

  @Column(name = "action_seq", updatable = false)
  private Integer actionSeq;

  @Column(name = "entry_order", updatable = false)
  private Integer entryOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_code", nullable = false, updatable = false)
  private BattleEventCode eventCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "param_animal_side", updatable = false)
  private BattleSide paramAnimalSide;

  @Enumerated(EnumType.STRING)
  @Column(name = "param_skill", updatable = false)
  private CardSkill paramSkill;

  @Enumerated(EnumType.STRING)
  @Column(name = "param_winner_side", updatable = false)
  private BattleSide paramWinnerSide;

  @Column(name = "param_points", updatable = false)
  private Integer paramPoints;

  private BattleBroadcastEvent(
      Long battleId,
      Integer eventSeq,
      Integer actionSeq,
      Integer entryOrder,
      BattleEventCode eventCode,
      BattleSide paramAnimalSide,
      CardSkill paramSkill,
      BattleSide paramWinnerSide,
      Integer paramPoints) {
    this.battleId = Objects.requireNonNull(battleId);
    this.eventSeq = validateEventSeq(eventSeq);
    this.actionSeq = actionSeq;
    this.entryOrder = entryOrder;
    this.eventCode = Objects.requireNonNull(eventCode);
    this.paramAnimalSide = paramAnimalSide;
    this.paramSkill = paramSkill;
    this.paramWinnerSide = paramWinnerSide;
    this.paramPoints = paramPoints;
    BattleBroadcastEventSpec.validate(
        eventCode, paramAnimalSide, paramSkill, paramWinnerSide, paramPoints);
  }

  public static BattleBroadcastEvent record(
      Long battleId,
      Integer eventSeq,
      Integer actionSeq,
      Integer entryOrder,
      BattleEventCode eventCode,
      BattleSide paramAnimalSide,
      CardSkill paramSkill,
      BattleSide paramWinnerSide,
      Integer paramPoints) {
    return new BattleBroadcastEvent(
        battleId,
        eventSeq,
        actionSeq,
        entryOrder,
        eventCode,
        paramAnimalSide,
        paramSkill,
        paramWinnerSide,
        paramPoints);
  }

  private static Integer validateEventSeq(Integer eventSeq) {
    Objects.requireNonNull(eventSeq);
    if (eventSeq < 1) {
      throw new IllegalArgumentException("Event seq is out of range: " + eventSeq);
    }
    return eventSeq;
  }
}
