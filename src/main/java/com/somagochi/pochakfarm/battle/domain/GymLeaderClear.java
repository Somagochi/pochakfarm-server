package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "gym_leader_clears",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_gym_leader_clears_user_leader",
          columnNames = {"user_id", "gym_leader_id"}),
      @UniqueConstraint(name = "uk_gym_leader_clears_battle_id", columnNames = "battle_id")
    })
public class GymLeaderClear extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "gym_leader_id", nullable = false, updatable = false)
  private Long gymLeaderId;

  @Column(name = "battle_id", nullable = false, updatable = false)
  private Long battleId;

  @Column(name = "gym_leader_coin_reward", nullable = false, updatable = false)
  private Long gymLeaderCoinReward;

  @Column(name = "experience_reward", nullable = false, updatable = false)
  private Long experienceReward;

  @Column(name = "badge_code", nullable = false, updatable = false, length = 64)
  private String badgeCode;

  @Column(name = "level_before", nullable = false, updatable = false)
  private Integer levelBefore;

  @Column(name = "level_after", nullable = false, updatable = false)
  private Integer levelAfter;

  @Column(name = "experience_after", nullable = false, updatable = false)
  private Long experienceAfter;

  @Column(name = "required_experience_for_next_level", nullable = false, updatable = false)
  private Long requiredExperienceForNextLevel;

  @Column(name = "level_up_coin_reward", nullable = false, updatable = false)
  private Long levelUpCoinReward;

  @Column(name = "coins_after", nullable = false, updatable = false)
  private Long coinsAfter;

  private GymLeaderClear(
      Long userId,
      Long gymLeaderId,
      Long battleId,
      long gymLeaderCoinReward,
      long experienceReward,
      String badgeCode,
      int levelBefore,
      int levelAfter,
      long experienceAfter,
      long requiredExperienceForNextLevel,
      long levelUpCoinReward,
      long coinsAfter) {
    this.userId = userId;
    this.gymLeaderId = gymLeaderId;
    this.battleId = battleId;
    this.gymLeaderCoinReward = gymLeaderCoinReward;
    this.experienceReward = experienceReward;
    this.badgeCode = badgeCode;
    this.levelBefore = levelBefore;
    this.levelAfter = levelAfter;
    this.experienceAfter = experienceAfter;
    this.requiredExperienceForNextLevel = requiredExperienceForNextLevel;
    this.levelUpCoinReward = levelUpCoinReward;
    this.coinsAfter = coinsAfter;
  }

  public static GymLeaderClear record(
      Long userId,
      Long gymLeaderId,
      Long battleId,
      long gymLeaderCoinReward,
      long experienceReward,
      String badgeCode,
      int levelBefore,
      int levelAfter,
      long experienceAfter,
      long requiredExperienceForNextLevel,
      long levelUpCoinReward,
      long coinsAfter) {
    return new GymLeaderClear(
        userId,
        gymLeaderId,
        battleId,
        gymLeaderCoinReward,
        experienceReward,
        badgeCode,
        levelBefore,
        levelAfter,
        experienceAfter,
        requiredExperienceForNextLevel,
        levelUpCoinReward,
        coinsAfter);
  }
}
