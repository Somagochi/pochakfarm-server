package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "battles",
    indexes =
        @Index(name = "idx_battles_user_id_started_at", columnList = "user_id, started_at desc"))
public class Battle extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "gym_leader_id", nullable = false, updatable = false)
  private Long gymLeaderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private BattleStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "result")
  private BattleResult result;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  private Battle(Long userId, Long gymLeaderId, Instant startedAt) {
    this.userId = Objects.requireNonNull(userId);
    this.gymLeaderId = Objects.requireNonNull(gymLeaderId);
    this.startedAt = Objects.requireNonNull(startedAt);
    this.status = BattleStatus.IN_PROGRESS;
  }

  public static Battle start(Long userId, Long gymLeaderId, Instant startedAt) {
    return new Battle(userId, gymLeaderId, startedAt);
  }

  public boolean isInProgress() {
    return status == BattleStatus.IN_PROGRESS;
  }

  public boolean isOwnedBy(Long userId) {
    return Objects.equals(this.userId, userId);
  }

  public void finish(BattleResult result, Instant endedAt) {
    requireInProgress();
    this.result = Objects.requireNonNull(result);
    this.endedAt = Objects.requireNonNull(endedAt);
    this.status = BattleStatus.FINISHED;
  }

  public void abandon(Instant endedAt) {
    requireInProgress();
    this.endedAt = Objects.requireNonNull(endedAt);
    this.status = BattleStatus.ABANDONED;
  }

  private void requireInProgress() {
    if (!isInProgress()) {
      throw new IllegalStateException("Battle is not in progress");
    }
  }
}
