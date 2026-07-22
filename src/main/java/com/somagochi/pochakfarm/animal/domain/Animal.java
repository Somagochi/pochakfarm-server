package com.somagochi.pochakfarm.animal.domain;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "animals")
public class Animal extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "animal_name", nullable = false)
  private String animalName;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type")
  private CardType cardType;

  @Column(name = "power")
  private Integer power;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_1")
  private CardSkill skill1;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_2")
  private CardSkill skill2;

  @Column(name = "card_no")
  private String cardNo;

  @Column(name = "result_image_key")
  private String cardImageKey;

  @Column(name = "cutout_image_key")
  private String animalImageKey;

  @Column(name = "provider")
  private String provider;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CharacterizationStatus status;

  @Column(name = "elapsed_ms")
  private Integer elapsedMs;

  @Column(name = "failure_reason")
  private String failureReason;
}
