package com.somagochi.pochakfarm.achievement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import com.somagochi.pochakfarm.achievement.dto.AchievementResponse;
import com.somagochi.pochakfarm.achievement.dto.AchievementRewardResponse;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRewardRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.UserAchievementRepository;
import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class AchievementQueryServiceIntegrationTest {

  @Autowired private AchievementQueryService achievementQueryService;
  @Autowired private AchievementRepository achievementRepository;
  @Autowired private AchievementRewardRepository achievementRewardRepository;
  @Autowired private UserAchievementRepository userAchievementRepository;
  @Autowired private BadgeRepository badgeRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Long userId;

  @BeforeEach
  void setUp() {
    cleanUp();
    userId =
        userRepository
            .save(
                User.register(
                    SocialProvider.KAKAO, UUID.randomUUID().toString(), UUID.randomUUID() + "@t"))
            .getId();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @Test
  void recordsAchievementWhenTargetIsReachedAndReportsProgressOtherwise() {
    persistAchievement("TEST_LEVEL_1", 1);
    persistAchievement("TEST_LEVEL_5", 5);

    CursorPage<AchievementResponse> response =
        achievementQueryService.getAchievements(userId, null, null);

    AchievementResponse reached = find(response, "TEST_LEVEL_1");
    assertTrue(reached.achieved());
    assertNotNull(reached.achievedAt());
    assertEquals(1L, reached.current());
    assertEquals(1L, reached.target());
    assertFalse(reached.rewardClaimed());

    AchievementResponse inProgress = find(response, "TEST_LEVEL_5");
    assertFalse(inProgress.achieved());
    assertNull(inProgress.achievedAt());
    assertEquals(1L, inProgress.current());
    assertEquals(5L, inProgress.target());

    assertEquals(1, userAchievementRepository.findByUserId(userId).size());
  }

  @Test
  void keepsAchievedAtStableAndDoesNotDuplicateOnRepeatedReads() {
    persistAchievement("TEST_LEVEL_1", 1);

    Instant first =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_LEVEL_1")
            .achievedAt();
    Instant second =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_LEVEL_1")
            .achievedAt();

    assertEquals(first, second);
    assertEquals(1, userAchievementRepository.findByUserId(userId).size());
  }

  @Test
  void hidesDisabledAchievementUntilItIsAlreadyAchieved() {
    Achievement disabled = persistAchievement("TEST_LEVEL_1", 1);
    disable(disabled.getId());

    assertTrue(achievementQueryService.getAchievements(userId, null, null).content().isEmpty());

    userAchievementRepository.save(UserAchievement.achieve(userId, disabled.getId()));

    AchievementResponse listed =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_LEVEL_1");
    assertTrue(listed.achieved());
  }

  @Test
  void excludesInvalidDefinitionInsteadOfFailingWholeList() {
    persistAchievement("TEST_LEVEL_1", 1);
    Achievement broken =
        achievementRepository.save(
            Achievement.create(
                "TEST_BROKEN",
                "잘못된 정의",
                null,
                AchievementCategory.CARD_TYPE,
                AchievementMetric.CAPTURE_COUNT_BY_CARD_TYPE,
                "OCEAN",
                10));

    CursorPage<AchievementResponse> response =
        achievementQueryService.getAchievements(userId, null, null);

    assertEquals(1, response.content().size());
    assertEquals("TEST_LEVEL_1", response.content().get(0).code());
    assertNotNull(broken.getId());
  }

  @Test
  void describesCoinAndBadgeRewardsWithBadgeMetadata() {
    Achievement achievement = persistAchievement("TEST_LEVEL_1", 1);
    badgeRepository.save(Badge.create("TEST_BADGE", "첫 걸음", "설명", "badges/first.png"));
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 100));
    achievementRewardRepository.save(AchievementReward.ofBadge(achievement.getId(), "TEST_BADGE"));

    List<AchievementRewardResponse> rewards =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_LEVEL_1").rewards();

    assertEquals(2, rewards.size());
    AchievementRewardResponse badge =
        rewards.stream().filter(reward -> reward.badgeName() != null).findFirst().orElseThrow();
    assertEquals("첫 걸음", badge.badgeName());
    assertNotNull(badge.badgeImageUrl());
  }

  @Test
  void dropsRewardRowsThatViolateRewardTypeContract() {
    Achievement achievement = persistAchievement("TEST_LEVEL_1", 1);
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 100));
    jdbcTemplate.update(
        "insert into achievement_rewards "
            + "(achievement_id, reward_type, reference_code, amount, created_at, updated_at) "
            + "values (?, 'COIN', null, null, current_timestamp, current_timestamp)",
        achievement.getId());

    List<AchievementRewardResponse> rewards =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_LEVEL_1").rewards();

    assertEquals(1, rewards.size());
    assertEquals(100L, rewards.get(0).amount());
  }

  @Test
  void pagesByCreatedAtAndFollowsNextCursor() {
    List<Achievement> saved = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      saved.add(persistAchievement("TEST_LEVEL_" + i, 99));
    }

    CursorPage<AchievementResponse> first =
        achievementQueryService.getAchievements(userId, null, null);

    assertEquals(20, first.content().size());
    assertTrue(first.hasNext());
    assertEquals(saved.get(19).getId(), first.nextCursor());
    assertEquals("TEST_LEVEL_1", first.content().get(0).code());

    CursorPage<AchievementResponse> second =
        achievementQueryService.getAchievements(userId, null, first.nextCursor());

    assertEquals(5, second.content().size());
    assertFalse(second.hasNext());
    assertNull(second.nextCursor());
    assertEquals("TEST_LEVEL_21", second.content().get(0).code());
  }

  @Test
  void filtersByCategory() {
    persistAchievement("TEST_LEVEL_1", 1);
    achievementRepository.save(
        Achievement.create(
            "TEST_TIER_S",
            "S 등급",
            null,
            AchievementCategory.TIER,
            AchievementMetric.CAPTURE_COUNT_BY_TIER,
            "S",
            5));

    CursorPage<AchievementResponse> levelOnly =
        achievementQueryService.getAchievements(userId, AchievementCategory.LEVEL, null);

    assertEquals(1, levelOnly.content().size());
    assertEquals("TEST_LEVEL_1", levelOnly.content().get(0).code());
    assertEquals(2, achievementQueryService.getAchievements(userId, null, null).content().size());
  }

  @Test
  void recordsAchievementsOutsideRequestedPage() {
    for (int i = 1; i <= 25; i++) {
      persistAchievement("TEST_LEVEL_" + i, 1);
    }

    CursorPage<AchievementResponse> first =
        achievementQueryService.getAchievements(userId, null, null);

    assertEquals(20, first.content().size());
    assertEquals(25, userAchievementRepository.findByUserId(userId).size());
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

  private void disable(Long achievementId) {
    jdbcTemplate.update("update achievements set enabled = false where id = ?", achievementId);
  }

  private AchievementResponse find(CursorPage<AchievementResponse> response, String code) {
    return response.content().stream()
        .filter(achievement -> achievement.code().equals(code))
        .findFirst()
        .orElseThrow();
  }

  private void cleanUp() {
    jdbcTemplate.update("delete from user_achievements");
    jdbcTemplate.update("delete from user_badges");
    jdbcTemplate.update("delete from achievement_rewards");
    jdbcTemplate.update("delete from achievements");
    jdbcTemplate.update("delete from badges");
  }
}
