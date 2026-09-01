package com.somagochi.pochakfarm.battle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.dto.BattleRewardResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleActionRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleBroadcastEventRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleEntryRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderClearRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.LevelRewardPolicy;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.CoinHistoryRepository;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BattleRewardServiceTest {

  private static final Instant STARTED_AT = Instant.parse("2026-09-01T00:00:00Z");

  @Autowired private BattleRewardService battleRewardService;
  @Autowired private BattleRepository battleRepository;
  @Autowired private BattleEntryRepository battleEntryRepository;
  @Autowired private BattleActionRepository battleActionRepository;
  @Autowired private BattleBroadcastEventRepository battleBroadcastEventRepository;
  @Autowired private GymLeaderRepository gymLeaderRepository;
  @Autowired private GymLeaderAnimalRepository gymLeaderAnimalRepository;
  @Autowired private GymLeaderClearRepository gymLeaderClearRepository;
  @Autowired private BadgeRepository badgeRepository;
  @Autowired private UserBadgeRepository userBadgeRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private CoinHistoryRepository coinHistoryRepository;
  @Autowired private LevelRewardPolicy levelRewardPolicy;
  @Autowired private EntityManager entityManager;

  private User user;

  @BeforeEach
  void setUp() {
    coinHistoryRepository.deleteAll();
    userBadgeRepository.deleteAll();
    gymLeaderClearRepository.deleteAll();
    battleActionRepository.deleteAll();
    battleBroadcastEventRepository.deleteAll();
    battleEntryRepository.deleteAll();
    battleRepository.deleteAll();
    gymLeaderAnimalRepository.deleteAll();
    gymLeaderRepository.deleteAllInBatch();
    badgeRepository.deleteAll();
    userRepository.deleteAll();
    user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                "보샅213"));
  }

  @Test
  void grantsFirstClearCoinExperienceBadgeAndLevelUpCoinInOneTransaction() {
    user.gainExperience(35, levelRewardPolicy);
    Battle battle = startFirstGymBattle();
    GymLeader gymLeader = gymLeaderRepository.findById(battle.getGymLeaderId()).orElseThrow();
    badgeRepository.save(Badge.create(gymLeader.getBadgeCode(), "관장 도전장", "첫 번째 관장 승리", null));
    flushAndClear();

    BattleRewardResponse response = battleRewardService.grantFirstClear(battle);

    assertTrue(response.firstClear());
    assertTrue(response.rewardGranted());
    assertEquals(300, response.gymLeaderCoins());
    assertEquals(20, response.experience());
    assertEquals(1, response.levelBefore());
    assertEquals(2, response.levelAfter());
    assertEquals(15, response.experienceAfter());
    assertEquals(500, response.levelUpCoins());
    assertEquals(1_800, response.coinsAfter());
    assertTrue(
        userBadgeRepository.findOwnedBadgeCodes(user.getId()).contains(gymLeader.getBadgeCode()));
    assertEquals(1, gymLeaderClearRepository.count());
    assertEquals(2, coinHistoryRepository.count());
  }

  @Test
  void retryOfSameBattleReturnsStoredRewardAndRematchWinGrantsNothing() {
    Battle firstBattle = startFirstGymBattle();
    GymLeader gymLeader = gymLeaderRepository.findById(firstBattle.getGymLeaderId()).orElseThrow();
    badgeRepository.save(Badge.create(gymLeader.getBadgeCode(), "관장 도전장", "첫 번째 관장 승리", null));
    flushAndClear();

    BattleRewardResponse first = battleRewardService.grantFirstClear(firstBattle);
    BattleRewardResponse retried = battleRewardService.grantFirstClear(firstBattle);
    Battle rematch =
        battleRepository.save(
            Battle.start(
                user.getId(), gymLeader.getId(), UUID.randomUUID().toString(), STARTED_AT));
    BattleRewardResponse rematchResult = battleRewardService.grantFirstClear(rematch);

    assertEquals(first, retried);
    assertFalse(rematchResult.firstClear());
    assertFalse(rematchResult.rewardGranted());
    assertEquals(0, rematchResult.gymLeaderCoins());
    assertEquals(0, rematchResult.experience());
    assertEquals(1, gymLeaderClearRepository.count());
    assertEquals(1, coinHistoryRepository.count());
  }

  @Test
  void sumsEveryLevelUpCoinWhenOneRewardRaisesMultipleLevels() {
    Battle battle = startGymBattle(8);
    GymLeader gymLeader = gymLeaderRepository.findById(battle.getGymLeaderId()).orElseThrow();
    badgeRepository.save(Badge.create(gymLeader.getBadgeCode(), "관장 도전장", "여덟 번째 관장 승리", null));
    flushAndClear();

    BattleRewardResponse response = battleRewardService.grantFirstClear(battle);

    assertEquals(1, response.levelBefore());
    assertEquals(5, response.levelAfter());
    assertEquals(0, response.experienceAfter());
    assertEquals(2_500, response.levelUpCoins());
    assertEquals(6_500, response.coinsAfter());
    assertEquals(2, coinHistoryRepository.count());
  }

  private Battle startFirstGymBattle() {
    return startGymBattle(1);
  }

  private Battle startGymBattle(int challengeOrder) {
    return new BattleFixture(
            battleRepository, battleEntryRepository, gymLeaderRepository, gymLeaderAnimalRepository)
        .challengeOrder(challengeOrder)
        .start(user.getId(), STARTED_AT);
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
