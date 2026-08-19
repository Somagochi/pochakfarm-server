package com.somagochi.pochakfarm.farm.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.dto.FarmFloorPurchaseResponse;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import com.somagochi.pochakfarm.user.application.UserCoinService;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import com.somagochi.pochakfarm.user.domain.Coin;
import com.somagochi.pochakfarm.user.domain.CoinTransactionReason;
import com.somagochi.pochakfarm.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FarmFloorPurchaseServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long SPACE_ID = 100L;

  private final FarmSpaceRepository farmSpaceRepository = mock(FarmSpaceRepository.class);
  private final UserQueryService userQueryService = mock(UserQueryService.class);
  private final UserCoinService userCoinService = mock(UserCoinService.class);
  private final FarmFloorPurchaseService service =
      new FarmFloorPurchaseService(farmSpaceRepository, userQueryService, userCoinService);

  @Test
  void unlocksNextFloorAndSpendsCoins() {
    FarmSpace space = givenSpace(FarmSpace.FIRST_FLOOR);
    User user = givenLockedUser(1_500L);
    given(
            userCoinService.spend(
                user,
                FarmSpace.FLOOR_UNLOCK_PRICE,
                CoinTransactionReason.FARM_FLOOR_PURCHASE,
                SPACE_ID))
        .willReturn(userWithCoins(500L));

    FarmFloorPurchaseResponse response = service.purchaseNextFloor(USER_ID, CardType.SEA);

    assertEquals(CardType.SEA, response.type());
    assertEquals(FarmSpace.FIRST_FLOOR + 1, response.unlockedFloor());
    assertEquals(500L, response.remainingCoins());
    assertEquals(FarmSpace.FIRST_FLOOR + 1, space.getFloor());
  }

  @Test
  void throwsWhenSpaceMissing() {
    given(farmSpaceRepository.findByUserIdAndType(USER_ID, CardType.SEA))
        .willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.purchaseNextFloor(USER_ID, CardType.SEA));

    assertEquals(ErrorCode.FARM_SPACE_NOT_FOUND.getCode(), exception.getCode());
    verifyNoInteractions(userCoinService);
  }

  @Test
  void throwsWhenAllFloorsAlreadyUnlocked() {
    givenSpace(FarmSpace.TOTAL_FLOOR_COUNT);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.purchaseNextFloor(USER_ID, CardType.SEA));

    assertEquals(ErrorCode.FARM_FLOOR_MAX_REACHED.getCode(), exception.getCode());
    verifyNoInteractions(userCoinService);
  }

  @Test
  void propagatesInsufficientCoins() {
    givenSpace(FarmSpace.FIRST_FLOOR);
    User user = givenLockedUser(500L);
    given(
            userCoinService.spend(
                user,
                FarmSpace.FLOOR_UNLOCK_PRICE,
                CoinTransactionReason.FARM_FLOOR_PURCHASE,
                SPACE_ID))
        .willThrow(new BusinessException(ErrorCode.INSUFFICIENT_COINS));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.purchaseNextFloor(USER_ID, CardType.SEA));

    assertEquals(ErrorCode.INSUFFICIENT_COINS.getCode(), exception.getCode());
  }

  private FarmSpace givenSpace(int unlockedFloor) {
    FarmSpace space = FarmSpace.create(USER_ID, CardType.SEA);
    setField(space, "id", SPACE_ID);
    setField(space, "floor", unlockedFloor);
    given(farmSpaceRepository.findByUserIdAndType(USER_ID, CardType.SEA))
        .willReturn(Optional.of(space));
    return space;
  }

  private User givenLockedUser(long coins) {
    User user = userWithCoins(coins);
    given(userQueryService.getForUpdate(USER_ID)).willReturn(user);
    return user;
  }

  private User userWithCoins(long coins) {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com", "포착이");
    setField(user, "id", USER_ID);
    setField(user, "coins", Coin.of(coins));
    return user;
  }
}
