package com.somagochi.pochakfarm.coupon.domain;

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
    name = "coupons",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_coupons_coupon_code",
            columnNames = {"coupon_code"}))
@SQLRestriction("deleted_at is null")
public class Coupon extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "coupon_code", nullable = false, updatable = false, length = 64)
  private String couponCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private CouponStatus status;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  private Coupon(String couponCode, Instant expiresAt) {
    this.couponCode = Objects.requireNonNull(couponCode);
    this.expiresAt = Objects.requireNonNull(expiresAt);
    this.status = CouponStatus.ACTIVE;
  }

  public static Coupon issue(String couponCode, Instant expiresAt) {
    return new Coupon(couponCode, expiresAt);
  }

  public boolean isUsed() {
    return status == CouponStatus.USED;
  }

  public boolean isExpired(Instant now) {
    return status == CouponStatus.EXPIRED || expiresAt.isBefore(now);
  }

  public void use() {
    this.status = CouponStatus.USED;
  }
}
