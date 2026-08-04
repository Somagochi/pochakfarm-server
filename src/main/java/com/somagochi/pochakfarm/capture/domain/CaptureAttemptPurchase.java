package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "capture_attempt_purchases",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_capture_attempt_purchases_user_request",
            columnNames = {"user_id", "client_request_id"}))
public class CaptureAttemptPurchase extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "client_request_id", nullable = false, updatable = false, length = 36)
  private String clientRequestId;

  private CaptureAttemptPurchase(Long userId, String clientRequestId) {
    this.userId = Objects.requireNonNull(userId);
    this.clientRequestId = Objects.requireNonNull(clientRequestId);
  }

  public static CaptureAttemptPurchase create(Long userId, String clientRequestId) {
    return new CaptureAttemptPurchase(userId, clientRequestId);
  }
}
