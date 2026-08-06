package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.Coin;
import com.somagochi.pochakfarm.user.domain.CoinHistory;
import com.somagochi.pochakfarm.user.domain.CoinTransactionReason;
import com.somagochi.pochakfarm.user.domain.CoinTransactionType;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.CoinHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserCoinServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long REFERENCE_ID = 100L;

  private final EntityManager entityManager = mock(EntityManager.class);
  private final CoinHistoryRepository coinHistoryRepository = mock(CoinHistoryRepository.class);
  private final UserCoinService service = new UserCoinService(entityManager, coinHistoryRepository);

  @Test
  void spendsCoinsAndRecordsHistory() {
    User user = lockedUser(1_500L);

    User result =
        service.spend(user, 1_000L, CoinTransactionReason.FARM_FLOOR_PURCHASE, REFERENCE_ID);

    assertEquals(500L, result.getCoins());
    ArgumentCaptor<CoinHistory> captor = ArgumentCaptor.forClass(CoinHistory.class);
    verify(coinHistoryRepository).save(captor.capture());
    CoinHistory history = captor.getValue();
    assertEquals(USER_ID, history.getUserId());
    assertEquals(CoinTransactionType.SPEND, history.getType());
    assertEquals(CoinTransactionReason.FARM_FLOOR_PURCHASE, history.getReason());
    assertEquals(1_000L, history.getAmount());
    assertEquals(500L, history.getBalanceAfter());
    assertEquals(REFERENCE_ID, history.getReferenceId());
  }

  @Test
  void earnsCoinsAndRecordsHistory() {
    User user = lockedUser(1_000L);

    User result = service.earn(user, 500L, CoinTransactionReason.LEVEL_UP_REWARD, REFERENCE_ID);

    assertEquals(1_500L, result.getCoins());
    ArgumentCaptor<CoinHistory> captor = ArgumentCaptor.forClass(CoinHistory.class);
    verify(coinHistoryRepository).save(captor.capture());
    CoinHistory history = captor.getValue();
    assertEquals(USER_ID, history.getUserId());
    assertEquals(CoinTransactionType.EARN, history.getType());
    assertEquals(CoinTransactionReason.LEVEL_UP_REWARD, history.getReason());
    assertEquals(500L, history.getAmount());
    assertEquals(1_500L, history.getBalanceAfter());
    assertEquals(REFERENCE_ID, history.getReferenceId());
  }

  @Test
  void recordsHistoryWithoutReference() {
    User user = lockedUser(1_000L);

    service.spend(user, 1_000L, CoinTransactionReason.FARM_FLOOR_PURCHASE, null);

    ArgumentCaptor<CoinHistory> captor = ArgumentCaptor.forClass(CoinHistory.class);
    verify(coinHistoryRepository).save(captor.capture());
    assertNull(captor.getValue().getReferenceId());
    assertEquals(0L, captor.getValue().getBalanceAfter());
  }

  @Test
  void throwsWhenUserNotLockedOnSpend() {
    User user = user(1_000L, LockModeType.NONE);

    assertThrows(
        IllegalStateException.class,
        () -> service.spend(user, 100L, CoinTransactionReason.FARM_FLOOR_PURCHASE, REFERENCE_ID));

    assertEquals(1_000L, user.getCoins());
    verifyNoInteractions(coinHistoryRepository);
  }

  @Test
  void throwsWhenUserNotLockedOnEarn() {
    User user = user(1_000L, LockModeType.OPTIMISTIC);

    assertThrows(
        IllegalStateException.class,
        () -> service.earn(user, 100L, CoinTransactionReason.LEVEL_UP_REWARD, REFERENCE_ID));

    assertEquals(1_000L, user.getCoins());
    verifyNoInteractions(coinHistoryRepository);
  }

  @Test
  void throwsWhenCoinsInsufficient() {
    User user = lockedUser(999L);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.spend(
                    user, 1_000L, CoinTransactionReason.FARM_FLOOR_PURCHASE, REFERENCE_ID));

    assertEquals(ErrorCode.INSUFFICIENT_COINS.getCode(), exception.getCode());
    assertEquals(999L, user.getCoins());
    verifyNoInteractions(coinHistoryRepository);
  }

  @Test
  void throwsWhenAmountIsNotPositive() {
    User user = lockedUser(1_000L);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.spend(user, 0L, CoinTransactionReason.FARM_FLOOR_PURCHASE, REFERENCE_ID));

    assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), exception.getCode());
    verifyNoInteractions(coinHistoryRepository);
  }

  private User lockedUser(long coins) {
    return user(coins, LockModeType.PESSIMISTIC_WRITE);
  }

  private User user(long coins, LockModeType lockMode) {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com");
    setField(user, "id", USER_ID);
    setField(user, "coins", Coin.of(coins));
    given(entityManager.getLockMode(user)).willReturn(lockMode);
    return user;
  }
}
