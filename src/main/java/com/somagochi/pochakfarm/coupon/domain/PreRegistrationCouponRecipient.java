package com.somagochi.pochakfarm.coupon.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "pre_registration_coupon_recipients",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_pre_registration_coupon_recipients_coupon_id",
          columnNames = {"coupon_id"}),
      @UniqueConstraint(
          name = "uk_pre_registration_coupon_recipients_user_id",
          columnNames = {"user_id"}),
      @UniqueConstraint(
          name = "uk_pre_registration_coupon_recipients_pre_registration_id",
          columnNames = {"pre_registration_id"})
    })
@SQLRestriction("deleted_at is null")
public class PreRegistrationCouponRecipient extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "coupon_id", nullable = false, updatable = false)
  private Long couponId;

  @Column(name = "pre_registration_id", nullable = false, updatable = false)
  private Long preRegistrationId;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "capture_id")
  private Long captureId;

  @Column(name = "converted_at")
  private Instant convertedAt;

  private PreRegistrationCouponRecipient(Long couponId, Long preRegistrationId) {
    this.couponId = Objects.requireNonNull(couponId);
    this.preRegistrationId = Objects.requireNonNull(preRegistrationId);
  }

  public static PreRegistrationCouponRecipient issue(Long couponId, Long preRegistrationId) {
    return new PreRegistrationCouponRecipient(couponId, preRegistrationId);
  }

  public boolean isConverted() {
    return convertedAt != null;
  }

  public boolean isAssigned() {
    return captureId != null;
  }

  public boolean isAssignedTo(Long userId) {
    return isAssigned() && Objects.equals(this.userId, userId);
  }

  public void assign(Long userId, Long captureId) {
    this.userId = Objects.requireNonNull(userId);
    this.captureId = Objects.requireNonNull(captureId);
  }

  public void convert(Instant convertedAt) {
    this.convertedAt = Objects.requireNonNull(convertedAt);
  }
}
