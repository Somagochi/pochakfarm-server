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
  private long captureIdSequence;

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
    convertPreRegistrationCoupon(userId);
    persistAchievement("TEST_SQUAD", AchievementMetric.PRE_REGISTRATION_CONVERTED, 1);
    persistAchievement("TEST_ONE_TYPE", AchievementMetric.MAX_OWNED_COUNT_PER_TYPE, 10);

    CursorPage<AchievementResponse> response =
        achievementQueryService.getAchievements(userId, null, null);

    AchievementResponse reached = find(response, "TEST_SQUAD");
    assertTrue(reached.achieved());
    assertNotNull(reached.achievedInfo().achievedAt());
    assertEquals(1L, reached.progress().current());
    assertEquals(1L, reached.progress().target());
    assertFalse(reached.achievedInfo().rewardClaimed());

    AchievementResponse inProgress = find(response, "TEST_ONE_TYPE");
    assertFalse(inProgress.achieved());
    assertNull(inProgress.achievedInfo());
    assertEquals(0L, inProgress.progress().current());
    assertEquals(10L, inProgress.progress().target());

    assertEquals(1, userAchievementRepository.findByUserId(userId).size());
  }

  @Test
  void computesCollectionAndFarmMetricsFromOwnedAnimals() {
    Long groundSpaceId = insertFarmSpace(userId, "GROUND");
    Long skySpaceId = insertFarmSpace(userId, "SKY");
    insertAnimal(groundSpaceId, 1, 1);
    insertAnimal(groundSpaceId, 4, 4);
    insertAnimal(skySpaceId, 1, 2);
    persistAchievement("TEST_FIRST_HOME", AchievementMetric.PLACED_ANIMAL_COUNT, 1);
    persistAchievement("TEST_ONE_TYPE", AchievementMetric.MAX_OWNED_COUNT_PER_TYPE, 10);
    persistAchievement("TEST_COLLECTOR", AchievementMetric.OWNED_TYPE_COUNT, 4);
    persistAchievement("TEST_START_END", AchievementMetric.ONLY_START_END_PLACED, 1);

    CursorPage<AchievementResponse> response =
        achievementQueryService.getAchievements(userId, null, null);

    AchievementResponse firstHome = find(response, "TEST_FIRST_HOME");
    assertTrue(firstHome.achieved());
    assertEquals(3L, firstHome.progress().current());

    assertEquals(2L, find(response, "TEST_ONE_TYPE").progress().current());
    assertEquals(2L, find(response, "TEST_COLLECTOR").progress().current());

    AchievementResponse startEnd = find(response, "TEST_START_END");
    assertTrue(startEnd.achieved());
    assertEquals(1L, startEnd.progress().current());
  }

  @Test
  void startEndIsNotSatisfiedWhenSameSpaceHasAnotherAnimal() {
    Long groundSpaceId = insertFarmSpace(userId, "GROUND");
    insertAnimal(groundSpaceId, 1, 1);
    insertAnimal(groundSpaceId, 4, 4);
    insertAnimal(groundSpaceId, 2, 1);
    persistAchievement("TEST_START_END", AchievementMetric.ONLY_START_END_PLACED, 1);

    CursorPage<AchievementResponse> response =
        achievementQueryService.getAchievements(userId, null, null);

    AchievementResponse startEnd = find(response, "TEST_START_END");
    assertFalse(startEnd.achieved());
    assertEquals(0L, startEnd.progress().current());
  }

  @Test
  void masksHiddenAchievementUntilAchievedAndRevealsAfterward() {
    Achievement hiddenAchievement =
        achievementRepository.save(
            Achievement.createHidden(
                "TEST_HIDDEN",
                "시작과 끝",
                "숨김 조건",
                AchievementCategory.FARM,
                AchievementMetric.PRE_REGISTRATION_CONVERTED,
                null,
                1));
    achievementRewardRepository.save(AchievementReward.ofCoin(hiddenAchievement.getId(), 1000));

    AchievementResponse locked =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_HIDDEN");
    assertTrue(locked.hidden());
    assertFalse(locked.achieved());
    assertNull(locked.achievedInfo());
    assertNull(locked.title());
    assertNull(locked.description());
    assertNull(locked.progress());
    assertNull(locked.imageUrl());
    assertNull(locked.rewards());

    convertPreRegistrationCoupon(userId);

    AchievementResponse revealed =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_HIDDEN");
    assertTrue(revealed.hidden());
    assertTrue(revealed.achieved());
    assertNotNull(revealed.achievedInfo());
    assertEquals("시작과 끝", revealed.title());
    assertEquals("숨김 조건", revealed.description());
    assertEquals(1L, revealed.progress().current());
    assertEquals(1L, revealed.progress().target());
    assertEquals(1, revealed.rewards().size());
  }

  @Test
  void buildsAchievementImageUrlsFromStoredKeys() {
    achievementRepository.save(
        Achievement.create(
            "TEST_WITH_IMAGES",
            "이미지 업적",
            null,
            AchievementCategory.EVENT,
            AchievementMetric.PRE_REGISTRATION_CONVERTED,
            null,
            1,
            "achievements/locked.png",
            "achievements/done.png"));
    persistAchievement("TEST_WITHOUT_IMAGES", AchievementMetric.PRE_REGISTRATION_CONVERTED, 1);

    AchievementResponse locked =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_WITH_IMAGES");
    assertNotNull(locked.imageUrl());
    assertTrue(locked.imageUrl().contains("achievements/locked.png"));

    convertPreRegistrationCoupon(userId);

    AchievementResponse done =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_WITH_IMAGES");
    assertNotNull(done.imageUrl());
    assertTrue(done.imageUrl().contains("achievements/done.png"));

    AchievementResponse withoutImages =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_WITHOUT_IMAGES");
    assertNull(withoutImages.imageUrl());
  }

  @Test
  void keepsAchievedAtStableAndDoesNotDuplicateOnRepeatedReads() {
    convertPreRegistrationCoupon(userId);
    persistAchievement("TEST_SQUAD", AchievementMetric.PRE_REGISTRATION_CONVERTED, 1);

    Instant first =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_SQUAD")
            .achievedInfo()
            .achievedAt();
    Instant second =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_SQUAD")
            .achievedInfo()
            .achievedAt();

    assertEquals(first, second);
    assertEquals(1, userAchievementRepository.findByUserId(userId).size());
  }

  @Test
  void hidesDisabledAchievementUntilItIsAlreadyAchieved() {
    Achievement disabled =
        persistAchievement("TEST_SQUAD", AchievementMetric.PRE_REGISTRATION_CONVERTED, 1);
    disable(disabled.getId());

    assertTrue(achievementQueryService.getAchievements(userId, null, null).content().isEmpty());

    userAchievementRepository.save(UserAchievement.achieve(userId, disabled.getId()));

    AchievementResponse listed =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_SQUAD");
    assertTrue(listed.achieved());
  }

  @Test
  void excludesInvalidDefinitionInsteadOfFailingWholeList() {
    persistAchievement("TEST_SQUAD", AchievementMetric.PRE_REGISTRATION_CONVERTED, 1);
    Achievement broken =
        achievementRepository.save(
            Achievement.create(
                "TEST_BROKEN",
                "잘못된 정의",
                null,
                AchievementCategory.COLLECTION,
                AchievementMetric.MAX_OWNED_COUNT_PER_TYPE,
                "GROUND",
                10));

    CursorPage<AchievementResponse> response =
        achievementQueryService.getAchievements(userId, null, null);

    assertEquals(1, response.content().size());
    assertEquals("TEST_SQUAD", response.content().get(0).code());
    assertNotNull(broken.getId());
  }

  @Test
  void describesCoinAndBadgeRewardsWithBadgeMetadata() {
    Achievement achievement =
        persistAchievement("TEST_SQUAD", AchievementMetric.PRE_REGISTRATION_CONVERTED, 1);
    badgeRepository.save(Badge.create("TEST_BADGE", "첫 걸음", "설명", "badges/first.png"));
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 100));
    achievementRewardRepository.save(AchievementReward.ofBadge(achievement.getId(), "TEST_BADGE"));

    List<AchievementRewardResponse> rewards =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_SQUAD").rewards();

    assertEquals(2, rewards.size());
    AchievementRewardResponse badge =
        rewards.stream().filter(reward -> reward.badgeName() != null).findFirst().orElseThrow();
    assertEquals("첫 걸음", badge.badgeName());
    assertNotNull(badge.badgeImageUrl());
  }

  @Test
  void dropsRewardRowsThatViolateRewardTypeContract() {
    Achievement achievement =
        persistAchievement("TEST_SQUAD", AchievementMetric.PRE_REGISTRATION_CONVERTED, 1);
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 100));
    jdbcTemplate.update(
        "insert into achievement_rewards "
            + "(achievement_id, reward_type, reference_code, amount, created_at, updated_at) "
            + "values (?, 'COIN', null, null, current_timestamp, current_timestamp)",
        achievement.getId());

    List<AchievementRewardResponse> rewards =
        find(achievementQueryService.getAchievements(userId, null, null), "TEST_SQUAD").rewards();

    assertEquals(1, rewards.size());
    assertEquals(100L, rewards.get(0).amount());
  }

  @Test
  void pagesByCreatedAtAndFollowsNextCursor() {
    List<Achievement> saved = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      saved.add(
          persistAchievement("TEST_ONE_TYPE_" + i, AchievementMetric.MAX_OWNED_COUNT_PER_TYPE, 99));
    }

    CursorPage<AchievementResponse> first =
        achievementQueryService.getAchievements(userId, null, null);

    assertEquals(20, first.content().size());
    assertTrue(first.hasNext());
    assertEquals(saved.get(19).getId(), first.nextCursor());
    assertEquals(saved.get(0).getId(), first.content().get(0).id());
    assertEquals("TEST_ONE_TYPE_1", first.content().get(0).code());

    CursorPage<AchievementResponse> second =
        achievementQueryService.getAchievements(userId, null, first.nextCursor());

    assertEquals(5, second.content().size());
    assertFalse(second.hasNext());
    assertNull(second.nextCursor());
    assertEquals("TEST_ONE_TYPE_21", second.content().get(0).code());
  }

  @Test
  void filtersByCategory() {
    persistAchievement("TEST_SQUAD", AchievementMetric.PRE_REGISTRATION_CONVERTED, 1);
    achievementRepository.save(
        Achievement.create(
            "TEST_COLLECTOR",
            "골고루 수집가",
            null,
            AchievementCategory.COLLECTION,
            AchievementMetric.OWNED_TYPE_COUNT,
            null,
            4));

    CursorPage<AchievementResponse> eventOnly =
        achievementQueryService.getAchievements(userId, AchievementCategory.EVENT, null);

    assertEquals(1, eventOnly.content().size());
    assertEquals("TEST_SQUAD", eventOnly.content().get(0).code());
    assertEquals(2, achievementQueryService.getAchievements(userId, null, null).content().size());
  }

  @Test
  void recordsAchievementsOutsideRequestedPage() {
    convertPreRegistrationCoupon(userId);
    for (int i = 1; i <= 25; i++) {
      persistAchievement("TEST_SQUAD_" + i, AchievementMetric.PRE_REGISTRATION_CONVERTED, 1);
    }

    CursorPage<AchievementResponse> first =
        achievementQueryService.getAchievements(userId, null, null);

    assertEquals(20, first.content().size());
    assertEquals(25, userAchievementRepository.findByUserId(userId).size());
  }

  private Achievement persistAchievement(String code, AchievementMetric metric, long target) {
    return achievementRepository.save(
        Achievement.create(code, code, null, AchievementCategory.EVENT, metric, null, target));
  }

  private void convertPreRegistrationCoupon(Long userId) {
    jdbcTemplate.update(
        "insert into pre_registration_coupon_recipients "
            + "(coupon_id, pre_registration_id, user_id, capture_id, converted_at, created_at, updated_at) "
            + "values (?, ?, ?, 0, current_timestamp, current_timestamp, current_timestamp)",
        userId,
        userId,
        userId);
  }

  private Long insertFarmSpace(Long userId, String type) {
    jdbcTemplate.update(
        "insert into farm_spaces (user_id, type, floor, version, created_at, updated_at) "
            + "values (?, ?, 4, 0, current_timestamp, current_timestamp)",
        userId,
        type);
    return jdbcTemplate.queryForObject(
        "select id from farm_spaces where user_id = ? and type = ?", Long.class, userId, type);
  }

  private void insertAnimal(Long spaceId, int floorNum, int slotNum) {
    jdbcTemplate.update(
        "insert into animals (capture_id, space_id, floor_num, slot_num, version, created_at, updated_at) "
            + "values (?, ?, ?, ?, 0, current_timestamp, current_timestamp)",
        ++captureIdSequence,
        spaceId,
        floorNum,
        slotNum);
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
    jdbcTemplate.update("delete from animals");
    jdbcTemplate.update("delete from farm_spaces");
    jdbcTemplate.update("delete from pre_registration_coupon_recipients");
  }
}
