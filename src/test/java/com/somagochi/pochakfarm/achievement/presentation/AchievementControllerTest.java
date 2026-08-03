package com.somagochi.pochakfarm.achievement.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.achievement.application.AchievementClaimService;
import com.somagochi.pochakfarm.achievement.application.AchievementQueryService;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.achievement.dto.AchievementClaimResponse;
import com.somagochi.pochakfarm.achievement.dto.AchievementResponse;
import com.somagochi.pochakfarm.achievement.dto.AchievementRewardResponse;
import com.somagochi.pochakfarm.common.config.SecurityConfig;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.exception.GlobalExceptionHandler;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationToken;
import com.somagochi.pochakfarm.common.security.SecurityAccessDeniedHandler;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AchievementController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  AchievementControllerTest.TestConfig.class
})
class AchievementControllerTest {

  private static final Long USER_ID = 1L;
  private static final Instant ACHIEVED_AT = Instant.parse("2026-07-29T00:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AchievementQueryService achievementQueryService;

  @MockitoBean private AchievementClaimService achievementClaimService;

  @Test
  void returnsAchievementsWithProgressAndRewards() throws Exception {
    given(achievementQueryService.getAchievements(USER_ID, null, null))
        .willReturn(
            CursorPage.of(
                List.of(
                    new AchievementResponse(
                        3L,
                        "ONE_TYPE_FOCUS",
                        "한 우물 포착",
                        "같은 타입의 동물을 10마리 이상 보유한다",
                        AchievementCategory.COLLECTION,
                        false,
                        "https://cdn.example.com/achievements/done.png",
                        new AchievementResponse.Progress(12, 10),
                        true,
                        new AchievementResponse.AchievedInfo(ACHIEVED_AT, false),
                        List.of(
                            new AchievementRewardResponse(RewardType.COIN, 100L, null, null),
                            new AchievementRewardResponse(
                                RewardType.BADGE,
                                null,
                                "첫 걸음",
                                "https://cdn.example.com/badge.png"))),
                    new AchievementResponse(
                        5L,
                        "START_AND_END",
                        null,
                        null,
                        AchievementCategory.FARM,
                        true,
                        null,
                        null,
                        false,
                        null,
                        null)),
                20L,
                true));

    mockMvc
        .perform(get("/api/achievements").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.nextCursor").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.content[0].id").value(3))
        .andExpect(jsonPath("$.data.content[0].code").value("ONE_TYPE_FOCUS"))
        .andExpect(jsonPath("$.data.content[0].category").value("COLLECTION"))
        .andExpect(jsonPath("$.data.content[0].hidden").value(false))
        .andExpect(
            jsonPath("$.data.content[0].imageUrl")
                .value("https://cdn.example.com/achievements/done.png"))
        .andExpect(jsonPath("$.data.content[0].progress.current").value(12))
        .andExpect(jsonPath("$.data.content[0].progress.target").value(10))
        .andExpect(jsonPath("$.data.content[0].achieved").value(true))
        .andExpect(jsonPath("$.data.content[0].achievedInfo.achievedAt").exists())
        .andExpect(jsonPath("$.data.content[0].achievedInfo.rewardClaimed").value(false))
        .andExpect(jsonPath("$.data.content[0].rewards[0].type").value("COIN"))
        .andExpect(jsonPath("$.data.content[0].rewards[0].amount").value(100))
        .andExpect(jsonPath("$.data.content[0].rewards[0].badgeName").doesNotExist())
        .andExpect(jsonPath("$.data.content[0].rewards[1].amount").doesNotExist())
        .andExpect(jsonPath("$.data.content[0].rewards[1].badgeName").value("첫 걸음"))
        .andExpect(jsonPath("$.data.content[1].id").value(5))
        .andExpect(jsonPath("$.data.content[1].hidden").value(true))
        .andExpect(jsonPath("$.data.content[1].title").doesNotExist())
        .andExpect(jsonPath("$.data.content[1].imageUrl").doesNotExist())
        .andExpect(jsonPath("$.data.content[1].progress").doesNotExist())
        .andExpect(jsonPath("$.data.content[1].achieved").value(false))
        .andExpect(jsonPath("$.data.content[1].achievedInfo").doesNotExist())
        .andExpect(jsonPath("$.data.content[1].rewards").doesNotExist());
  }

  @Test
  void passesCategoryAndCursorToQueryService() throws Exception {
    given(achievementQueryService.getAchievements(USER_ID, AchievementCategory.FARM, 20L))
        .willReturn(CursorPage.of(List.of(), null, false));

    mockMvc
        .perform(
            get("/api/achievements")
                .param("category", "FARM")
                .param("cursor", "20")
                .with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void returnsBadRequestForUnsupportedCategory() throws Exception {
    mockMvc
        .perform(
            get("/api/achievements")
                .param("category", "NOPE")
                .with(authentication(userAuthentication())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
  }

  @Test
  void returnsClaimedRewardsAndUpdatedBalance() throws Exception {
    given(achievementClaimService.claim(USER_ID, "ONE_TYPE_FOCUS"))
        .willReturn(
            new AchievementClaimResponse(
                "ONE_TYPE_FOCUS",
                List.of(new AchievementRewardResponse(RewardType.COIN, 100L, null, null)),
                350,
                80));

    mockMvc
        .perform(
            post("/api/achievements/{code}/claim", "ONE_TYPE_FOCUS")
                .with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("ONE_TYPE_FOCUS"))
        .andExpect(jsonPath("$.data.rewards[0].type").value("COIN"))
        .andExpect(jsonPath("$.data.coins").value(350))
        .andExpect(jsonPath("$.data.experience").value(80));
  }

  @Test
  void returnsConflictWhenRewardAlreadyClaimed() throws Exception {
    willThrow(new BusinessException(ErrorCode.ACHIEVEMENT_REWARD_ALREADY_CLAIMED))
        .given(achievementClaimService)
        .claim(USER_ID, "ONE_TYPE_FOCUS");

    mockMvc
        .perform(
            post("/api/achievements/{code}/claim", "ONE_TYPE_FOCUS")
                .with(authentication(userAuthentication())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ACHIEVEMENT_REWARD_ALREADY_CLAIMED"));
  }

  @Test
  void returnsBadRequestWhenAchievementNotAchieved() throws Exception {
    willThrow(new BusinessException(ErrorCode.ACHIEVEMENT_NOT_ACHIEVED))
        .given(achievementClaimService)
        .claim(USER_ID, "ONE_TYPE_FOCUS");

    mockMvc
        .perform(
            post("/api/achievements/{code}/claim", "ONE_TYPE_FOCUS")
                .with(authentication(userAuthentication())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ACHIEVEMENT_NOT_ACHIEVED"));
  }

  @Test
  void returnsNotFoundForUnknownAchievementCode() throws Exception {
    willThrow(new BusinessException(ErrorCode.ACHIEVEMENT_NOT_FOUND))
        .given(achievementClaimService)
        .claim(USER_ID, "UNKNOWN");

    mockMvc
        .perform(
            post("/api/achievements/{code}/claim", "UNKNOWN")
                .with(authentication(userAuthentication())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ACHIEVEMENT_NOT_FOUND"));
  }

  @Test
  void returnsUnauthorizedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/achievements")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(post("/api/achievements/{code}/claim", "ONE_TYPE_FOCUS"))
        .andExpect(status().isUnauthorized());
  }

  private static Authentication userAuthentication() {
    return new JwtAuthenticationToken("token", new UserPrincipal(USER_ID), null);
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter() {
      return new JwtAuthenticationFilter(null) {
        @Override
        protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
          filterChain.doFilter(request, response);
        }
      };
    }
  }
}
