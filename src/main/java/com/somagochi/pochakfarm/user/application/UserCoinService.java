package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.user.domain.CoinHistory;
import com.somagochi.pochakfarm.user.domain.CoinTransactionReason;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.CoinHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCoinService {

  private final EntityManager entityManager;
  private final CoinHistoryRepository coinHistoryRepository;

  public UserCoinService(EntityManager entityManager, CoinHistoryRepository coinHistoryRepository) {
    this.entityManager = entityManager;
    this.coinHistoryRepository = coinHistoryRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public User spend(User user, long amount, CoinTransactionReason reason, Long referenceId) {
    validateLocked(user);
    user.spendCoins(amount);
    coinHistoryRepository.save(
        CoinHistory.spend(user.getId(), amount, user.getCoins(), reason, referenceId));
    return user;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public User earn(User user, long amount, CoinTransactionReason reason, Long referenceId) {
    validateLocked(user);
    user.addCoins(amount);
    coinHistoryRepository.save(
        CoinHistory.earn(user.getId(), amount, user.getCoins(), reason, referenceId));
    return user;
  }

  private void validateLocked(User user) {
    if (entityManager.getLockMode(user) != LockModeType.PESSIMISTIC_WRITE) {
      throw new IllegalStateException(
          "User must be locked with PESSIMISTIC_WRITE before coin transaction");
    }
  }
}
