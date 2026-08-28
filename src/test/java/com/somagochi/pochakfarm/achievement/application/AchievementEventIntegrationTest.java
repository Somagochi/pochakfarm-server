package com.somagochi.pochakfarm.achievement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.dto.AchievementReconciliationResult;
import com.somagochi.pochakfarm.achievement.dto.AchievementResponse;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.UserAchievementRepository;
import com.somagochi.pochakfarm.animal.application.AnimalSlotMoveService;
import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.PreRegistrationCouponRecipientRepository;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class AchievementEventIntegrationTest {

  @Autowired private AchievementRepository achievementRepository;
  @Autowired private UserAchievementRepository userAchievementRepository;
  @Autowired private AchievementQueryService achievementQueryService;
  @Autowired private AchievementReconciliationService achievementReconciliationService;
  @Autowired private PreRegistrationCouponRecipientRepository recipientRepository;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private AnimalSlotMoveService animalSlotMoveService;
  @Autowired private CaptureRepository captureRepository;
  @Autowired private FarmSpaceRepository farmSpaceRepository;
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
                    SocialProvider.KAKAO,
                    UUID.randomUUID().toString(),
                    UUID.randomUUID() + "@test.com",
                    "u" + UUID.randomUUID().toString().substring(0, 5)))
            .getId();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @Test
  void recordsPreRegistrationAchievementWhenCouponRecipientIsConverted() {
    Achievement achievement =
        persistAchievement(AchievementMetric.PRE_REGISTRATION_CONVERTED, false);
    PreRegistrationCouponRecipient recipient =
        recipientRepository.save(PreRegistrationCouponRecipient.issue(1L, 1L));

    recipient.assign(userId, 1L);
    recipient.convert(Instant.now());
    recipientRepository.saveAndFlush(recipient);

    assertTrue(
        userAchievementRepository
            .findByUserIdAndAchievementId(userId, achievement.getId())
            .isPresent());
    AchievementResponse response =
        achievementQueryService.getAchievements(userId, null, null).content().get(0);
    assertTrue(response.achieved());
    assertEquals(1L, response.progress().current());
  }

  @Test
  void recordsHiddenStartEndAchievementWhenAnimalPositionChanges() {
    FarmSpace space = FarmSpace.create(userId, CardType.GROUND);
    space.unlockNextFloor();
    space.unlockNextFloor();
    space.unlockNextFloor();
    space = farmSpaceRepository.save(space);
    animalRepository.save(Animal.create(captureId(), space.getId(), 1, 1));
    Animal last = animalRepository.save(Animal.create(captureId(), space.getId(), 1, 2));
    Achievement achievement = persistAchievement(AchievementMetric.ONLY_START_END_PLACED, true);

    animalSlotMoveService.moveToSlot(userId, last.getId(), 4, 4);

    assertTrue(
        userAchievementRepository
            .findByUserIdAndAchievementId(userId, achievement.getId())
            .isPresent());
    AchievementResponse response =
        achievementQueryService.getAchievements(userId, null, null).content().get(0);
    assertTrue(response.hidden());
    assertTrue(response.achieved());
    assertEquals("TEST_ONLY_START_END_PLACED", response.title());

    animalSlotMoveService.moveToSlot(userId, last.getId(), 2, 1);

    AchievementResponse noLongerSatisfied =
        achievementQueryService.getAchievements(userId, null, null).content().get(0);
    assertTrue(noLongerSatisfied.achieved());
    assertEquals(0L, noLongerSatisfied.progress().current());
  }

  @Test
  void reconciliationBackfillsAchievementMissedByJpaEvents() {
    Achievement achievement =
        persistAchievement(AchievementMetric.PRE_REGISTRATION_CONVERTED, false);
    jdbcTemplate.update(
        "insert into pre_registration_coupon_recipients "
            + "(coupon_id, pre_registration_id, user_id, capture_id, converted_at, created_at, updated_at) "
            + "values (?, ?, ?, 0, current_timestamp, current_timestamp, current_timestamp)",
        userId,
        userId,
        userId);

    AchievementReconciliationResult result =
        achievementReconciliationService.reconcile(List.of(userId));

    assertEquals(1, result.succeededCount());
    assertTrue(result.failedUserIds().isEmpty());
    assertTrue(
        userAchievementRepository
            .findByUserIdAndAchievementId(userId, achievement.getId())
            .isPresent());
  }

  private Achievement persistAchievement(AchievementMetric metric, boolean hidden) {
    String code = "TEST_" + metric;
    if (hidden) {
      return achievementRepository.save(
          Achievement.createHidden(code, code, null, AchievementCategory.FARM, metric, null, 1));
    }
    return achievementRepository.save(
        Achievement.create(code, code, null, AchievementCategory.EVENT, metric, null, 1));
  }

  private Long captureId() {
    return captureRepository
        .save(
            Capture.create(
                userId,
                UUID.randomUUID().toString(),
                CardType.GROUND,
                Tier.C,
                AnimalName.from("이벤트동물"),
                CardSkill.GROUND_PAW_STRIKE,
                CardSkill.GROUND_LEAF_GUARD,
                "001",
                "captures/original.png",
                "image/png",
                Instant.now().plusSeconds(300)))
        .getId();
  }

  private void cleanUp() {
    jdbcTemplate.update("delete from user_achievements");
    jdbcTemplate.update("delete from achievement_rewards");
    jdbcTemplate.update("delete from achievements");
    jdbcTemplate.update("delete from pre_registration_coupon_recipients");
    jdbcTemplate.update("delete from animals");
    jdbcTemplate.update("delete from farm_spaces");
    jdbcTemplate.update("delete from captures");
  }
}
