package com.somagochi.pochakfarm.user.domain;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coin {

  @Column(name = "coins", nullable = false)
  private long value;

  private Coin(long value) {
    if (value < 0) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    this.value = value;
  }

  public static Coin of(long value) {
    return new Coin(value);
  }

  public Coin add(long amount) {
    validatePositive(amount);
    return new Coin(Math.addExact(value, amount));
  }

  public Coin spend(long amount) {
    validatePositive(amount);
    if (!canAfford(amount)) {
      throw new BusinessException(ErrorCode.INSUFFICIENT_COINS);
    }
    return new Coin(value - amount);
  }

  public boolean canAfford(long amount) {
    return value >= amount;
  }

  public long value() {
    return value;
  }

  private void validatePositive(long amount) {
    if (amount <= 0) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Coin that)) {
      return false;
    }
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
