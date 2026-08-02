package com.somagochi.pochakfarm.achievement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import com.somagochi.pochakfarm.achievement.dto.AchievementClaimResponse;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRewardRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.UserAchievementRepository;
import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class AchievementClaimServiceIntegrationTest {

  @Autowired private AchievementClaimService achievementClaimService;
  @Autowired private AchievementRepository achievementRepository;
  @Autowired private AchievementRewardRepository achievementRewardRepository;
  @Autowired private UserAchievementRepository userAchievementRepository;
  @Autowired private BadgeRepository badgeRepository;
  @Autowired private UserBadgeRepository userBadgeRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Long userId;
  private long initialCoins;

  @BeforeEach
  void setUp() {
    cleanUp();
    User user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO, UUID.randomUUID().toString(), UUID.randomUUID() + "@t"));
    userId = user.getId();
    initialCoins = user.getCoins();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @Test
  void grantsEveryRewardOfAchievementInOneClaim() {
    Achievement achievement = persistAchievement("TEST_LEVEL_1", 1);
    badgeRepository.save(Badge.create("TEST_BADGE", "첫 걸음", null, "badges/first.png"));
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 100));
    achievementRewardRepository.save(AchievementReward.ofExperience(achievement.getId(), 30));
    achievementRewardRepository.save(AchievementReward.ofBadge(achievement.getId(), "TEST_BADGE"));

    AchievementClaimResponse response = achievementClaimService.claim(userId, "TEST_LEVEL_1");

    assertEquals("TEST_LEVEL_1", response.code());
    assertEquals(3, response.rewards().size());
    assertEquals(initialCoins + 100, response.coins());
    assertEquals(30L, response.experience());

    User reloaded = userRepository.findById(userId).orElseThrow();
    assertEquals(initialCoins + 100, reloaded.getCoins());
    assertEquals(30L, reloaded.getExperience());
    assertEquals(1, userBadgeRepository.findByUserId(userId).size());
    assertNotNull(
        userAchievementRepository
            .findByUserIdAndAchievementId(userId, achievement.getId())
            .orElseThrow()
            .getRewardClaimedAt());
  }

  @Test
  void claimsWithoutListingAchievementsFirst() {
    Achievement achievement = persistAchievement("TEST_LEVEL_1", 1);
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 50));
    assertTrue(userAchievementRepository.findByUserId(userId).isEmpty());

    AchievementClaimResponse response = achievementClaimService.claim(userId, "TEST_LEVEL_1");

    assertEquals(initialCoins + 50, response.coins());
    assertEquals(1, userAchievementRepository.findByUserId(userId).size());
  }

  @Test
  void rejectsSecondClaimOfSameAchievement() {
    Achievement achievement = persistAchievement("TEST_LEVEL_1", 1);
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 100));
    achievementClaimService.claim(userId, "TEST_LEVEL_1");

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> achievementClaimService.claim(userId, "TEST_LEVEL_1"));

    assertEquals(ErrorCode.ACHIEVEMENT_REWARD_ALREADY_CLAIMED.getCode(), exception.getCode());
    assertEquals(initialCoins + 100, userRepository.findById(userId).orElseThrow().getCoins());
  }

  @Test
  void rejectsClaimOfUnachievedAchievement() {
    Achievement achievement = persistAchievement("TEST_LEVEL_5", 5);
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 100));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> achievementClaimService.claim(userId, "TEST_LEVEL_5"));

    assertEquals(ErrorCode.ACHIEVEMENT_NOT_ACHIEVED.getCode(), exception.getCode());
    assertTrue(userAchievementRepository.findByUserId(userId).isEmpty());
    assertEquals(initialCoins, userRepository.findById(userId).orElseThrow().getCoins());
  }

  @Test
  void rejectsUnknownAchievementCode() {
    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> achievementClaimService.claim(userId, "TEST_UNKNOWN"));

    assertEquals(ErrorCode.ACHIEVEMENT_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void rollsBackClaimMarkWhenRewardGrantFails() {
    Achievement achievement = persistAchievement("TEST_LEVEL_1", 1);
    userAchievementRepository.save(UserAchievement.achieve(userId, achievement.getId()));
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 100));
    achievementRewardRepository.save(
        AchievementReward.ofBadge(achievement.getId(), "TEST_MISSING"));

    assertThrows(
        BusinessException.class, () -> achievementClaimService.claim(userId, "TEST_LEVEL_1"));

    assertEquals(initialCoins, userRepository.findById(userId).orElseThrow().getCoins());
    assertFalse(
        userAchievementRepository
            .findByUserIdAndAchievementId(userId, achievement.getId())
            .orElseThrow()
            .isRewardClaimed());
  }

  @Test
  void doesNotDuplicateBadgeAlreadyOwned() {
    Achievement first = persistAchievement("TEST_LEVEL_1", 1);
    Achievement second = persistAchievement("TEST_LEVEL_1_AGAIN", 1);
    badgeRepository.save(Badge.create("TEST_BADGE", "첫 걸음", null, null));
    achievementRewardRepository.save(AchievementReward.ofBadge(first.getId(), "TEST_BADGE"));
    achievementRewardRepository.save(AchievementReward.ofBadge(second.getId(), "TEST_BADGE"));

    achievementClaimService.claim(userId, "TEST_LEVEL_1");
    achievementClaimService.claim(userId, "TEST_LEVEL_1_AGAIN");

    assertEquals(1, userBadgeRepository.findByUserId(userId).size());
  }

  private Achievement persistAchievement(String code, long target) {
    return achievementRepository.save(
        Achievement.create(
            code,
            code,
            null,
            AchievementCategory.LEVEL,
            AchievementMetric.USER_LEVEL,
            null,
            target));
  }

  private void cleanUp() {
    jdbcTemplate.update("delete from user_achievements");
    jdbcTemplate.update("delete from user_badges");
    jdbcTemplate.update("delete from achievement_rewards");
    jdbcTemplate.update("delete from achievements");
    jdbcTemplate.update("delete from badges");
  }
}
