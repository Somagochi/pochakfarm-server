package com.somagochi.pochakfarm.battle.domain;

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
    name = "battle_entries",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_battle_entries_battle_id_side_order_no",
          columnNames = {"battle_id", "side", "order_no"}),
      @UniqueConstraint(
          name = "uk_battle_entries_battle_id_capture_id",
          columnNames = {"battle_id", "capture_id"})
    })
public class BattleEntry extends BaseEntity {

  public static final int ENTRY_COUNT = 3;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "battle_id", nullable = false, updatable = false)
  private Long battleId;

  @Enumerated(EnumType.STRING)
  @Column(name = "side", nullable = false, updatable = false)
  private BattleSide side;

  @Column(name = "order_no", nullable = false, updatable = false)
  private Integer orderNo;

  @Column(name = "capture_id", updatable = false)
  private Long captureId;

  @Column(name = "gym_leader_animal_id", updatable = false)
  private Long gymLeaderAnimalId;

  private BattleEntry(
      Long battleId, BattleSide side, Integer orderNo, Long captureId, Long gymLeaderAnimalId) {
    this.battleId = Objects.requireNonNull(battleId);
    this.side = Objects.requireNonNull(side);
    this.orderNo = validateOrderNo(orderNo);
    this.captureId = captureId;
    this.gymLeaderAnimalId = gymLeaderAnimalId;
  }

  public static BattleEntry ofUser(Long battleId, Integer orderNo, Long captureId) {
    return new BattleEntry(
        battleId, BattleSide.USER, orderNo, Objects.requireNonNull(captureId), null);
  }

  public static BattleEntry ofNpc(Long battleId, Integer orderNo, Long gymLeaderAnimalId) {
    return new BattleEntry(
        battleId, BattleSide.NPC, orderNo, null, Objects.requireNonNull(gymLeaderAnimalId));
  }

  private static Integer validateOrderNo(Integer orderNo) {
    Objects.requireNonNull(orderNo);
    if (orderNo < 1 || orderNo > ENTRY_COUNT) {
      throw new IllegalArgumentException("Order no is out of range: " + orderNo);
    }
    return orderNo;
  }
}
