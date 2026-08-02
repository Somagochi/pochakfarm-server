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
                        "LEVEL_10",
                        "레벨 10 달성",
                        "레벨 10에 도달하세요",
                        AchievementCategory.LEVEL,
                        12,
                        10,
                        true,
                        ACHIEVED_AT,
                        false,
                        List.of(
                            new AchievementRewardResponse(RewardType.COIN, 100L, null, null),
                            new AchievementRewardResponse(
                                RewardType.BADGE,
                                null,
                                "첫 걸음",
                                "https://cdn.example.com/badge.png"))),
                    new AchievementResponse(
                        "CAPTURE_TIER_S_5",
                        "S 등급 5마리",
                        null,
                        AchievementCategory.TIER,
                        2,
                        5,
                        false,
                        null,
                        false,
                        List.of())),
                20L,
                true));

    mockMvc
        .perform(get("/api/achievements").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.nextCursor").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.content[0].code").value("LEVEL_10"))
        .andExpect(jsonPath("$.data.content[0].category").value("LEVEL"))
        .andExpect(jsonPath("$.data.content[0].current").value(12))
        .andExpect(jsonPath("$.data.content[0].target").value(10))
        .andExpect(jsonPath("$.data.content[0].achieved").value(true))
        .andExpect(jsonPath("$.data.content[0].rewardClaimed").value(false))
        .andExpect(jsonPath("$.data.content[0].rewards[0].type").value("COIN"))
        .andExpect(jsonPath("$.data.content[0].rewards[0].amount").value(100))
        .andExpect(jsonPath("$.data.content[0].rewards[1].badgeName").value("첫 걸음"))
        .andExpect(jsonPath("$.data.content[1].achieved").value(false))
        .andExpect(jsonPath("$.data.content[1].achievedAt").doesNotExist());
  }

  @Test
  void passesCategoryAndCursorToQueryService() throws Exception {
    given(achievementQueryService.getAchievements(USER_ID, AchievementCategory.TIER, 20L))
        .willReturn(CursorPage.of(List.of(), null, false));

    mockMvc
        .perform(
            get("/api/achievements")
                .param("category", "TIER")
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
    given(achievementClaimService.claim(USER_ID, "LEVEL_10"))
        .willReturn(
            new AchievementClaimResponse(
                "LEVEL_10",
                List.of(new AchievementRewardResponse(RewardType.COIN, 100L, null, null)),
                350,
                80));

    mockMvc
        .perform(
            post("/api/achievements/{code}/claim", "LEVEL_10")
                .with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("LEVEL_10"))
        .andExpect(jsonPath("$.data.rewards[0].type").value("COIN"))
        .andExpect(jsonPath("$.data.coins").value(350))
        .andExpect(jsonPath("$.data.experience").value(80));
  }

  @Test
  void returnsConflictWhenRewardAlreadyClaimed() throws Exception {
    willThrow(new BusinessException(ErrorCode.ACHIEVEMENT_REWARD_ALREADY_CLAIMED))
        .given(achievementClaimService)
        .claim(USER_ID, "LEVEL_10");

    mockMvc
        .perform(
            post("/api/achievements/{code}/claim", "LEVEL_10")
                .with(authentication(userAuthentication())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ACHIEVEMENT_REWARD_ALREADY_CLAIMED"));
  }

  @Test
  void returnsBadRequestWhenAchievementNotAchieved() throws Exception {
    willThrow(new BusinessException(ErrorCode.ACHIEVEMENT_NOT_ACHIEVED))
        .given(achievementClaimService)
        .claim(USER_ID, "LEVEL_10");

    mockMvc
        .perform(
            post("/api/achievements/{code}/claim", "LEVEL_10")
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
        .perform(post("/api/achievements/{code}/claim", "LEVEL_10"))
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
