package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.domain.CoinHistory;
import com.somagochi.pochakfarm.user.domain.CoinTransactionReason;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.CoinHistoryRepository;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCoinService {

  private final UserRepository userRepository;
  private final CoinHistoryRepository coinHistoryRepository;

  public UserCoinService(
      UserRepository userRepository, CoinHistoryRepository coinHistoryRepository) {
    this.userRepository = userRepository;
    this.coinHistoryRepository = coinHistoryRepository;
  }

  @Transactional
  public User spend(Long userId, long amount, CoinTransactionReason reason, Long referenceId) {
    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.spendCoins(amount);
    coinHistoryRepository.save(
        CoinHistory.spend(userId, amount, user.getCoins(), reason, referenceId));
    return user;
  }
}
