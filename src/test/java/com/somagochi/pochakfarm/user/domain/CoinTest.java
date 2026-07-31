package com.somagochi.pochakfarm.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class CoinTest {

  @Test
  void rejectsNegativeBalance() {
    BusinessException exception = assertThrows(BusinessException.class, () -> Coin.of(-1L));

    assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), exception.getCode());
  }

  @Test
  void spendSubtractsAmount() {
    Coin coin = Coin.of(1000L);

    Coin spent = coin.spend(300L);

    assertEquals(700L, spent.value());
    assertEquals(1000L, coin.value());
  }

  @Test
  void spendRejectsZeroOrNegativeAmount() {
    Coin coin = Coin.of(1000L);

    BusinessException exception = assertThrows(BusinessException.class, () -> coin.spend(0L));

    assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), exception.getCode());
    assertThrows(BusinessException.class, () -> coin.spend(-100L));
  }

  @Test
  void spendRejectsAmountOverBalance() {
    Coin coin = Coin.of(500L);

    BusinessException exception = assertThrows(BusinessException.class, () -> coin.spend(501L));

    assertEquals(ErrorCode.INSUFFICIENT_COINS.getCode(), exception.getCode());
  }

  @Test
  void spendAllowsExactBalance() {
    Coin coin = Coin.of(500L);

    assertEquals(0L, coin.spend(500L).value());
  }

  @Test
  void canAffordComparesAgainstBalance() {
    Coin coin = Coin.of(500L);

    assertTrue(coin.canAfford(500L));
    assertFalse(coin.canAfford(501L));
  }

  @Test
  void equalsByValue() {
    assertEquals(Coin.of(100L), Coin.of(100L));
  }
}
