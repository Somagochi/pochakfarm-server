package com.somagochi.pochakfarm.achievement.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRewardRepository;
import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationToken;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AchievementClaimE2eTest {

  private static final String CODE = "E2E_PRE_REGISTRATION";
  private static final String BADGE_CODE = "E2E_BADGE";
  private static final String ACHIEVED_IMAGE_KEY = "achievements/achieved.png";
  private static final String BADGE_IMAGE_KEY = "badges/e2e.png";

  @Autowired private MockMvc mockMvc;
  @Autowired private AchievementRepository achievementRepository;
  @Autowired private AchievementRewardRepository achievementRewardRepository;
  @Autowired private BadgeRepository badgeRepository;
  @Autowired private UserBadgeRepository userBadgeRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private FileStorage fileStorage;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Long userId;
  private long initialCoins;

  @BeforeEach
  void setUp() {
    cleanUp();
    User user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@t",
                "u" + UUID.randomUUID().toString().substring(0, 5)));
    userId = user.getId();
    initialCoins = user.getCoins();
    convertPreRegistrationCoupon(userId);
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @Test
  @DisplayName("정상 보상이 등록된 업적은 수령 시 코인이 늘어난다")
  void grantsCoinWhenRewardIsRegistered() throws Exception {
    Achievement achievement = persistAchievement();
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 1000));

    listAchievements()
        .andExpect(jsonPath("$.data.content[0].achieved").value(true))
        .andExpect(jsonPath("$.data.content[0].achievedInfo.rewardClaimed").value(false));

    claim()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.rewards.length()").value(1))
        .andExpect(jsonPath("$.data.coins").value(initialCoins + 1000));

    assertEquals(initialCoins + 1000, currentCoins());
    listAchievements()
        .andExpect(jsonPath("$.data.content[0].achievedInfo.rewardClaimed").value(true));
  }

  @Test
  @DisplayName("보상이 등록되지 않은 업적은 수령이 실패하고 수령 상태가 소진되지 않는다")
  void rejectsClaimWhenRewardIsMissing() throws Exception {
    persistAchievement();

    claim()
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("ACHIEVEMENT_REWARD_NOT_GRANTABLE"));

    assertEquals(initialCoins, currentCoins());
    listAchievements()
        .andExpect(jsonPath("$.data.content[0].achievedInfo.rewardClaimed").value(false));
  }

  @Test
  @DisplayName("코인 보상 amount 가 NULL 이면 수령이 실패하고 수령 상태가 소진되지 않는다")
  void rejectsClaimWhenCoinAmountIsNull() throws Exception {
    Achievement achievement = persistAchievement();
    insertRawCoinReward(achievement.getId(), null);

    claim()
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("ACHIEVEMENT_REWARD_NOT_GRANTABLE"));

    assertEquals(initialCoins, currentCoins());
    listAchievements()
        .andExpect(jsonPath("$.data.content[0].achievedInfo.rewardClaimed").value(false));
  }

  @Test
  @DisplayName("코인 보상 amount 가 0 이면 수령이 실패하고 수령 상태가 소진되지 않는다")
  void rejectsClaimWhenCoinAmountIsZero() throws Exception {
    Achievement achievement = persistAchievement();
    insertRawCoinReward(achievement.getId(), 0L);

    claim()
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("ACHIEVEMENT_REWARD_NOT_GRANTABLE"));

    assertEquals(initialCoins, currentCoins());
    listAchievements()
        .andExpect(jsonPath("$.data.content[0].achievedInfo.rewardClaimed").value(false));
  }

  @Test
  @DisplayName("뱃지가 없는 보상은 목록에서 제외되고 수령도 실패해 코인이 함께 롤백된다")
  void rejectsClaimWhenReferencedBadgeIsMissing() throws Exception {
    Achievement achievement = persistAchievement();
    achievementRewardRepository.save(AchievementReward.ofCoin(achievement.getId(), 1000));
    achievementRewardRepository.save(
        AchievementReward.ofBadge(achievement.getId(), "E2E_MISSING_BADGE"));

    listAchievements().andExpect(jsonPath("$.data.content[0].rewards.length()").value(1));

    claim()
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("ACHIEVEMENT_REWARD_NOT_GRANTABLE"));

    assertEquals(initialCoins, currentCoins());
    listAchievements()
        .andExpect(jsonPath("$.data.content[0].achievedInfo.rewardClaimed").value(false));
  }

  @Test
  @DisplayName("뱃지 이미지가 등록되어 있으면 수령 후 목록 imageUrl 이 뱃지 이미지로 바뀐다")
  void showsBadgeImageAfterClaimWhenBadgeImageExists() throws Exception {
    Achievement achievement = persistAchievement();
    badgeRepository.save(Badge.create(BADGE_CODE, "E2E 뱃지", null, BADGE_IMAGE_KEY));
    achievementRewardRepository.save(AchievementReward.ofBadge(achievement.getId(), BADGE_CODE));

    listAchievements()
        .andExpect(
            jsonPath("$.data.content[0].imageUrl").value(fileStorage.buildUrl(ACHIEVED_IMAGE_KEY)));

    claim().andExpect(status().isOk());

    assertEquals(1, userBadgeRepository.findByUserId(userId).size());
    listAchievements()
        .andExpect(jsonPath("$.data.content[0].achievedInfo.rewardClaimed").value(true))
        .andExpect(
            jsonPath("$.data.content[0].imageUrl").value(fileStorage.buildUrl(BADGE_IMAGE_KEY)));
  }

  @Test
  @DisplayName("뱃지 이미지가 비어 있으면 수령 후에도 목록 imageUrl 이 업적 이미지로 남는다")
  void fallsBackToAchievementImageAfterClaimWhenBadgeImageIsMissing() throws Exception {
    Achievement achievement = persistAchievement();
    badgeRepository.save(Badge.create(BADGE_CODE, "E2E 뱃지", null, null));
    achievementRewardRepository.save(AchievementReward.ofBadge(achievement.getId(), BADGE_CODE));

    claim().andExpect(status().isOk());

    listAchievements()
        .andExpect(jsonPath("$.data.content[0].achievedInfo.rewardClaimed").value(true))
        .andExpect(
            jsonPath("$.data.content[0].imageUrl").value(fileStorage.buildUrl(ACHIEVED_IMAGE_KEY)));
  }

  private ResultActions listAchievements() throws Exception {
    return mockMvc
        .perform(get("/api/achievements").with(authentication(userAuthentication())))
        .andExpect(status().isOk());
  }

  private ResultActions claim() throws Exception {
    return mockMvc.perform(
        post("/api/achievements/{code}/claim", CODE).with(authentication(userAuthentication())));
  }

  private Authentication userAuthentication() {
    return new JwtAuthenticationToken("token", new UserPrincipal(userId), null);
  }

  private long currentCoins() {
    return userRepository.findById(userId).orElseThrow().getCoins();
  }

  private Achievement persistAchievement() {
    return achievementRepository.save(
        Achievement.create(
            CODE,
            "사전예약 업적",
            null,
            AchievementCategory.EVENT,
            AchievementMetric.PRE_REGISTRATION_CONVERTED,
            null,
            1,
            null,
            ACHIEVED_IMAGE_KEY));
  }

  private void insertRawCoinReward(Long achievementId, Long amount) {
    jdbcTemplate.update(
        "insert into achievement_rewards "
            + "(achievement_id, reward_type, reference_code, amount, created_at, updated_at) "
            + "values (?, 'COIN', null, ?, current_timestamp, current_timestamp)",
        achievementId,
        amount);
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

  private void cleanUp() {
    jdbcTemplate.update("delete from user_achievements");
    jdbcTemplate.update("delete from user_badges");
    jdbcTemplate.update("delete from achievement_rewards");
    jdbcTemplate.update("delete from achievements");
    jdbcTemplate.update("delete from badges");
    jdbcTemplate.update("delete from pre_registration_coupon_recipients");
    jdbcTemplate.update("delete from coin_histories");
  }
}
