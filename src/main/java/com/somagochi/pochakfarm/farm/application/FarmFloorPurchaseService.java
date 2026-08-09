package com.somagochi.pochakfarm.farm.application;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.dto.FarmFloorPurchaseResponse;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import com.somagochi.pochakfarm.user.application.UserCoinService;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import com.somagochi.pochakfarm.user.domain.CoinTransactionReason;
import com.somagochi.pochakfarm.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FarmFloorPurchaseService {

  private final FarmSpaceRepository farmSpaceRepository;
  private final UserQueryService userQueryService;
  private final UserCoinService userCoinService;

  @Transactional
  public FarmFloorPurchaseResponse purchaseNextFloor(Long userId, CardType type) {
    FarmSpace space =
        farmSpaceRepository
            .findByUserIdAndType(userId, type)
            .orElseThrow(() -> new BusinessException(ErrorCode.FARM_SPACE_NOT_FOUND));
    int unlockedFloor = space.unlockNextFloor();
    User user =
        userCoinService.spend(
            userQueryService.getForUpdate(userId),
            FarmSpace.FLOOR_UNLOCK_PRICE,
            CoinTransactionReason.FARM_FLOOR_PURCHASE,
            space.getId());
    return new FarmFloorPurchaseResponse(type, unlockedFloor, user.getCoins());
  }
}
