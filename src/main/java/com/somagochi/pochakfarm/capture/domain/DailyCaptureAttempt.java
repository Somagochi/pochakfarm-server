package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "daily_capture_attempts",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_daily_capture_attempts_user_date",
            columnNames = {"user_id", "attempt_date"}))
public class DailyCaptureAttempt extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "attempt_date", nullable = false, updatable = false)
  private LocalDate attemptDate;

  @Column(name = "remaining", nullable = false)
  private int remaining;

  private DailyCaptureAttempt(Long userId, LocalDate attemptDate, int remaining) {
    this.userId = Objects.requireNonNull(userId);
    this.attemptDate = Objects.requireNonNull(attemptDate);
    if (remaining < 0) {
      throw new IllegalArgumentException("remaining must not be negative");
    }
    this.remaining = remaining;
  }

  public static DailyCaptureAttempt create(Long userId, LocalDate attemptDate, int remaining) {
    return new DailyCaptureAttempt(userId, attemptDate, remaining);
  }

  public void consume() {
    if (remaining == 0) {
      throw new BusinessException(ErrorCode.CAPTURE_ATTEMPT_REQUIRED);
    }
    remaining--;
  }

  public void purchase() {
    if (remaining > 0) {
      throw new BusinessException(ErrorCode.CAPTURE_ATTEMPT_ALREADY_AVAILABLE);
    }
    remaining++;
  }
}
