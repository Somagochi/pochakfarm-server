package com.somagochi.pochakfarm.battle.application;

import com.somagochi.pochakfarm.badge.application.BadgeGrantService;
import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.domain.GymLeaderClear;
import com.somagochi.pochakfarm.battle.dto.BattleRewardResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderClearRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.application.UserCoinService;
import com.somagochi.pochakfarm.user.domain.CoinTransactionReason;
import com.somagochi.pochakfarm.user.domain.LevelReward;
import com.somagochi.pochakfarm.user.domain.LevelRewardPolicy;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BattleRewardService {

  private final GymLeaderClearRepository gymLeaderClearRepository;
  private final GymLeaderRepository gymLeaderRepository;
  private final UserRepository userRepository;
  private final UserCoinService userCoinService;
  private final BadgeGrantService badgeGrantService;
  private final BattlePolicy battlePolicy;
  private final LevelRewardPolicy levelRewardPolicy;

  @Transactional(propagation = Propagation.MANDATORY)
  public BattleRewardResponse grantFirstClear(Battle battle) {
    User user = userForUpdate(battle.getUserId());
    return gymLeaderClearRepository
        .findByBattleId(battle.getId())
        .map(BattleRewardResponse::granted)
        .orElseGet(() -> grantWhenNotCleared(battle, user));
  }

  @Transactional(readOnly = true)
  public BattleRewardResponse findResult(Battle battle) {
    return gymLeaderClearRepository
        .findByBattleId(battle.getId())
        .map(BattleRewardResponse::granted)
        .orElseGet(
            () -> {
              User user = user(battle.getUserId());
              return BattleRewardResponse.notGranted(
                  user, levelRewardPolicy.requiredExperienceForNextLevel(user.getLevel()));
            });
  }

  private BattleRewardResponse grantWhenNotCleared(Battle battle, User user) {
    if (gymLeaderClearRepository.existsByUserIdAndGymLeaderId(
        battle.getUserId(), battle.getGymLeaderId())) {
      return BattleRewardResponse.notGranted(
          user, levelRewardPolicy.requiredExperienceForNextLevel(user.getLevel()));
    }

    GymLeader gymLeader = gymLeader(battle.getGymLeaderId());
    long coinReward = battlePolicy.gymLeaderCoinReward(gymLeader.getChallengeOrder());
    long experienceReward = battlePolicy.gymLeaderExperienceReward(gymLeader.getChallengeOrder());

    userCoinService.earn(
        user, coinReward, CoinTransactionReason.GYM_LEADER_CLEAR_REWARD, battle.getId());
    LevelReward levelReward = user.gainExperience(experienceReward, levelRewardPolicy);
    if (levelReward.coinReward() > 0) {
      userCoinService.earn(
          user, levelReward.coinReward(), CoinTransactionReason.LEVEL_UP_REWARD, battle.getId());
    }
    badgeGrantService.grant(user.getId(), gymLeader.getBadgeCode());

    GymLeaderClear clear =
        gymLeaderClearRepository.save(
            GymLeaderClear.record(
                user.getId(),
                gymLeader.getId(),
                battle.getId(),
                coinReward,
                levelReward.experienceReward(),
                gymLeader.getBadgeCode(),
                levelReward.levelBefore(),
                levelReward.levelAfter(),
                levelReward.experienceAfter(),
                levelReward.requiredExperienceForNextLevel(),
                levelReward.coinReward(),
                user.getCoins()));
    return BattleRewardResponse.granted(clear);
  }

  private GymLeader gymLeader(Long gymLeaderId) {
    return gymLeaderRepository
        .findById(gymLeaderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.GYM_LEADER_NOT_FOUND));
  }

  private User userForUpdate(Long userId) {
    return userRepository
        .findByIdForUpdate(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }

  private User user(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }
}
