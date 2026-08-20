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
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "gym_leader_animals",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_gym_leader_animals_gym_leader_id_order_no",
            columnNames = {"gym_leader_id", "order_no"}))
@SQLRestriction("deleted_at is null")
public class GymLeaderAnimal extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "gym_leader_id", nullable = false, updatable = false)
  private Long gymLeaderId;

  @Column(name = "order_no", nullable = false)
  private Integer orderNo;

  @Embedded private AnimalName animalName;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type", nullable = false)
  private CardType cardType;

  @Enumerated(EnumType.STRING)
  @Column(name = "tier", nullable = false)
  private Tier tier;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_1", nullable = false)
  private CardSkill skill1;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_2", nullable = false)
  private CardSkill skill2;

  @Column(name = "image_key")
  private String imageKey;

  private GymLeaderAnimal(
      Long gymLeaderId,
      Integer orderNo,
      AnimalName animalName,
      CardType cardType,
      Tier tier,
      CardSkill skill1,
      CardSkill skill2,
      String imageKey) {
    this.gymLeaderId = Objects.requireNonNull(gymLeaderId);
    this.orderNo = validateOrderNo(orderNo);
    this.animalName = Objects.requireNonNull(animalName);
    this.cardType = Objects.requireNonNull(cardType);
    this.tier = Objects.requireNonNull(tier);
    this.skill1 = Objects.requireNonNull(skill1);
    this.skill2 = Objects.requireNonNull(skill2);
    this.imageKey = imageKey;
  }

  public static GymLeaderAnimal create(
      Long gymLeaderId,
      Integer orderNo,
      AnimalName animalName,
      CardType cardType,
      Tier tier,
      CardSkill skill1,
      CardSkill skill2,
      String imageKey) {
    return new GymLeaderAnimal(
        gymLeaderId, orderNo, animalName, cardType, tier, skill1, skill2, imageKey);
  }

  public String getAnimalName() {
    return animalName.value();
  }

  public void changeImageKey(String imageKey) {
    this.imageKey = imageKey;
  }

  private static Integer validateOrderNo(Integer orderNo) {
    Objects.requireNonNull(orderNo);
    if (orderNo < 1 || orderNo > GymLeader.ANIMAL_COUNT) {
      throw new IllegalArgumentException("Order no is out of range: " + orderNo);
    }
    return orderNo;
  }
}
