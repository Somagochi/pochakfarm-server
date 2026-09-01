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
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
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
        @Index(name = "idx_battles_user_id_started_at", columnList = "user_id, started_at desc"),
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_battles_user_id_client_request_id",
            columnNames = {"user_id", "client_request_id"}))
public class Battle extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "gym_leader_id", nullable = false, updatable = false)
  private Long gymLeaderId;

  @Column(name = "client_request_id", nullable = false, updatable = false, length = 36)
  private String clientRequestId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private BattleStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "result")
  private BattleResult result;

  @Column(name = "bar_position", nullable = false)
  private Integer barPosition;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "last_action_at")
  private Instant lastActionAt;

  @Column(name = "final_ready_at")
  private Instant finalReadyAt;

  @Column(name = "final_expires_at")
  private Instant finalExpiresAt;

  @Column(name = "final_tap_count")
  private Integer finalTapCount;

  @Column(name = "final_points")
  private Integer finalPoints;

  @Column(name = "ended_at")
  private Instant endedAt;

  private Battle(Long userId, Long gymLeaderId, String clientRequestId, Instant startedAt) {
    this.userId = Objects.requireNonNull(userId);
    this.gymLeaderId = Objects.requireNonNull(gymLeaderId);
    this.clientRequestId = Objects.requireNonNull(clientRequestId);
    this.startedAt = Objects.requireNonNull(startedAt);
    this.status = BattleStatus.IN_PROGRESS;
    this.barPosition = BattlePolicy.INITIAL_BAR_POSITION;
  }

  public static Battle start(
      Long userId, Long gymLeaderId, String clientRequestId, Instant startedAt) {
    return new Battle(userId, gymLeaderId, clientRequestId, startedAt);
  }

  public boolean isInProgress() {
    return status == BattleStatus.IN_PROGRESS;
  }

  public boolean isOwnedBy(Long userId) {
    return Objects.equals(this.userId, userId);
  }

  public Instant lastProgressAt() {
    return lastActionAt == null ? startedAt : lastActionAt;
  }

  public boolean isExpiredAt(Instant now, Duration threshold) {
    return isInProgress() && !lastProgressAt().plus(threshold).isAfter(now);
  }

  public void applyAction(int barPosition, Instant actionAt) {
    requireInProgress();
    this.barPosition = barPosition;
    this.lastActionAt = Objects.requireNonNull(actionAt);
  }

  public void prepareFinalRound(Instant finalReadyAt) {
    requireInProgress();
    this.finalReadyAt = Objects.requireNonNull(finalReadyAt);
  }

  public boolean isFinalRoundReady() {
    return isInProgress() && finalReadyAt != null && finalExpiresAt == null;
  }

  public boolean isFinalRoundStartExpired(Instant now, Duration timeout) {
    return isFinalRoundReady() && !finalReadyAt.plus(timeout).isAfter(now);
  }

  public void startFinalRound(Instant finalExpiresAt) {
    requireInProgress();
    if (finalReadyAt == null) {
      throw new IllegalStateException("Battle final round is not ready");
    }
    if (this.finalExpiresAt == null) {
      this.finalExpiresAt = Objects.requireNonNull(finalExpiresAt);
    }
  }

  public boolean isFinalRoundStarted() {
    return finalExpiresAt != null;
  }

  public boolean isFinalRoundSubmissionExpired(Instant now, Duration submissionGrace) {
    return finalExpiresAt != null && finalExpiresAt.plus(submissionGrace).isBefore(now);
  }

  public void applyFinalRound(int finalTapCount, int finalPoints, int barPosition) {
    requireInProgress();
    if (finalExpiresAt == null) {
      throw new IllegalStateException("Battle final round is not started");
    }
    this.finalTapCount = finalTapCount;
    this.finalPoints = finalPoints;
    this.barPosition = barPosition;
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
