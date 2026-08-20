package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

  @Embedded private AnimalName animalName;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type", nullable = false, updatable = false)
  private CardType cardType;

  @Enumerated(EnumType.STRING)
  @Column(name = "tier", nullable = false, updatable = false)
  private Tier tier;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_1", nullable = false, updatable = false)
  private CardSkill skill1;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_2", nullable = false, updatable = false)
  private CardSkill skill2;

  private BattleEntry(
      Long battleId,
      BattleSide side,
      Integer orderNo,
      Long captureId,
      Long gymLeaderAnimalId,
      AnimalName animalName,
      CardType cardType,
      Tier tier,
      CardSkill skill1,
      CardSkill skill2) {
    this.battleId = Objects.requireNonNull(battleId);
    this.side = Objects.requireNonNull(side);
    this.orderNo = validateOrderNo(orderNo);
    this.captureId = captureId;
    this.gymLeaderAnimalId = gymLeaderAnimalId;
    this.animalName = Objects.requireNonNull(animalName);
    this.cardType = Objects.requireNonNull(cardType);
    this.tier = Objects.requireNonNull(tier);
    this.skill1 = Objects.requireNonNull(skill1);
    this.skill2 = Objects.requireNonNull(skill2);
  }

  public static BattleEntry ofUser(
      Long battleId,
      Integer orderNo,
      Long captureId,
      AnimalName animalName,
      CardType cardType,
      Tier tier,
      CardSkill skill1,
      CardSkill skill2) {
    return new BattleEntry(
        battleId,
        BattleSide.USER,
        orderNo,
        Objects.requireNonNull(captureId),
        null,
        animalName,
        cardType,
        tier,
        skill1,
        skill2);
  }

  public static BattleEntry ofNpc(Long battleId, GymLeaderAnimal gymLeaderAnimal) {
    Objects.requireNonNull(gymLeaderAnimal);
    return new BattleEntry(
        battleId,
        BattleSide.NPC,
        gymLeaderAnimal.getOrderNo(),
        null,
        Objects.requireNonNull(gymLeaderAnimal.getId()),
        AnimalName.from(gymLeaderAnimal.getAnimalName()),
        gymLeaderAnimal.getCardType(),
        gymLeaderAnimal.getTier(),
        gymLeaderAnimal.getSkill1(),
        gymLeaderAnimal.getSkill2());
  }

  public String getAnimalName() {
    return animalName.value();
  }

  public boolean isUserSide() {
    return side == BattleSide.USER;
  }

  private static Integer validateOrderNo(Integer orderNo) {
    Objects.requireNonNull(orderNo);
    if (orderNo < 1 || orderNo > ENTRY_COUNT) {
      throw new IllegalArgumentException("Order no is out of range: " + orderNo);
    }
    return orderNo;
  }
}
