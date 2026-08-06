package com.somagochi.pochakfarm.user.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "coin_histories",
    indexes =
        @Index(name = "idx_coin_histories_user_id_created_at", columnList = "user_id, created_at"))
public class CoinHistory extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, updatable = false)
  private CoinTransactionType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "reason", nullable = false, updatable = false)
  private CoinTransactionReason reason;

  @Column(name = "amount", nullable = false, updatable = false)
  private long amount;

  @Column(name = "balance_after", nullable = false, updatable = false)
  private long balanceAfter;

  @Column(name = "reference_id", updatable = false)
  private Long referenceId;

  private CoinHistory(
      Long userId,
      CoinTransactionType type,
      CoinTransactionReason reason,
      long amount,
      long balanceAfter,
      Long referenceId) {
    this.userId = userId;
    this.type = type;
    this.reason = reason;
    this.amount = amount;
    this.balanceAfter = balanceAfter;
    this.referenceId = referenceId;
  }

  public static CoinHistory spend(
      Long userId, long amount, long balanceAfter, CoinTransactionReason reason, Long referenceId) {
    if (amount <= 0) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    return new CoinHistory(
        userId, CoinTransactionType.SPEND, reason, amount, balanceAfter, referenceId);
  }

  public static CoinHistory earn(
      Long userId, long amount, long balanceAfter, CoinTransactionReason reason, Long referenceId) {
    if (amount <= 0) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    return new CoinHistory(
        userId, CoinTransactionType.EARN, reason, amount, balanceAfter, referenceId);
  }
}
