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
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserCoinServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long REFERENCE_ID = 100L;

  private final UserRepository userRepository = mock(UserRepository.class);
  private final CoinHistoryRepository coinHistoryRepository = mock(CoinHistoryRepository.class);
  private final UserCoinService service =
      new UserCoinService(userRepository, coinHistoryRepository);

  @Test
  void spendsCoinsAndRecordsHistory() {
    givenUser(1_500L);

    User result =
        service.spend(USER_ID, 1_000L, CoinTransactionReason.FARM_FLOOR_PURCHASE, REFERENCE_ID);

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
  void recordsHistoryWithoutReference() {
    givenUser(1_000L);

    service.spend(USER_ID, 1_000L, CoinTransactionReason.FARM_FLOOR_PURCHASE, null);

    ArgumentCaptor<CoinHistory> captor = ArgumentCaptor.forClass(CoinHistory.class);
    verify(coinHistoryRepository).save(captor.capture());
    assertNull(captor.getValue().getReferenceId());
    assertEquals(0L, captor.getValue().getBalanceAfter());
  }

  @Test
  void throwsWhenUserMissing() {
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.spend(
                    USER_ID, 1_000L, CoinTransactionReason.FARM_FLOOR_PURCHASE, REFERENCE_ID));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    verifyNoInteractions(coinHistoryRepository);
  }

  @Test
  void throwsWhenCoinsInsufficient() {
    User user = givenUser(999L);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.spend(
                    USER_ID, 1_000L, CoinTransactionReason.FARM_FLOOR_PURCHASE, REFERENCE_ID));

    assertEquals(ErrorCode.INSUFFICIENT_COINS.getCode(), exception.getCode());
    assertEquals(999L, user.getCoins());
    verifyNoInteractions(coinHistoryRepository);
  }

  @Test
  void throwsWhenAmountIsNotPositive() {
    givenUser(1_000L);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.spend(
                    USER_ID, 0L, CoinTransactionReason.FARM_FLOOR_PURCHASE, REFERENCE_ID));

    assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), exception.getCode());
    verifyNoInteractions(coinHistoryRepository);
  }

  private User givenUser(long coins) {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com");
    setField(user, "id", USER_ID);
    setField(user, "coins", Coin.of(coins));
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
    return user;
  }
}
