package com.somagochi.pochakfarm.battle.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.battle.application.BattleStartService;
import com.somagochi.pochakfarm.battle.application.GymLeaderQueryService;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.dto.BattleNpcEntryResponse;
import com.somagochi.pochakfarm.battle.dto.BattleRestResponse;
import com.somagochi.pochakfarm.battle.dto.BattleSkillResponse;
import com.somagochi.pochakfarm.battle.dto.BattleStartResponse;
import com.somagochi.pochakfarm.battle.dto.BattleUserEntryResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderAnimalResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderDetailResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderProfileResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderUnlockResponse;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import com.somagochi.pochakfarm.common.config.SecurityConfig;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.exception.GlobalExceptionHandler;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BattleController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  BattleControllerTest.TestConfig.class
})
class BattleControllerTest {

  private static final Long USER_ID = 1L;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GymLeaderQueryService gymLeaderQueryService;

  @MockitoBean private BattleStartService battleStartService;

  @Test
  void returnsGymLeaderListWithThumbnailAndUnlockedOnly() throws Exception {
    given(gymLeaderQueryService.getGymLeaders(USER_ID))
        .willReturn(
            List.of(new GymLeaderResponse(4L, "노바", 4, "https://cdn/thumb.png", false, false)));

    mockMvc
        .perform(get("/api/battles/gym-leaders").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].gymLeaderId").value(4))
        .andExpect(jsonPath("$.data[0].thumbnailUrl").value("https://cdn/thumb.png"))
        .andExpect(jsonPath("$.data[0].cleared").value(false))
        .andExpect(jsonPath("$.data[0].unlocked").value(false))
        .andExpect(jsonPath("$.data[0].code").doesNotExist())
        .andExpect(jsonPath("$.data[0].badgeCode").doesNotExist())
        .andExpect(jsonPath("$.data[0].imageUrl").doesNotExist())
        .andExpect(jsonPath("$.data[0].unlock").doesNotExist());
  }

  @Test
  void returnsSeparatedUnlockConditionsInDetailResponse() throws Exception {
    given(gymLeaderQueryService.getGymLeader(USER_ID, 4L))
        .willReturn(
            new GymLeaderDetailResponse(
                gymLeaderProfileResponse(false, true, "BDG008", false), List.of()));

    mockMvc
        .perform(get("/api/battles/gym-leaders/4").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.gymLeader.gymLeaderId").value(4))
        .andExpect(jsonPath("$.data.gymLeader.imageUrl").value("https://cdn/image.png"))
        .andExpect(jsonPath("$.data.gymLeader.unlock.unlocked").value(false))
        .andExpect(jsonPath("$.data.gymLeader.unlock.requiredLevel").value(12))
        .andExpect(jsonPath("$.data.gymLeader.unlock.levelSatisfied").value(true))
        .andExpect(jsonPath("$.data.gymLeader.unlock.previousBadgeCode").value("BDG008"))
        .andExpect(jsonPath("$.data.gymLeader.unlock.previousBadgeSatisfied").value(false));
  }

  @Test
  void hidesGymLeaderAnimalSkillsInDetailResponse() throws Exception {
    given(gymLeaderQueryService.getGymLeader(USER_ID, 4L))
        .willReturn(
            new GymLeaderDetailResponse(
                gymLeaderProfileResponse(true, true, "BDG008", true),
                List.of(new GymLeaderAnimalResponse(1, "별콩", CardType.SPACE, Tier.B, null))));

    mockMvc
        .perform(get("/api/battles/gym-leaders/4").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.animals[0].animalName").value("별콩"))
        .andExpect(jsonPath("$.data.animals[0].tier").value("B"))
        .andExpect(jsonPath("$.data.animals[0].skill1").doesNotExist())
        .andExpect(jsonPath("$.data.animals[0].skill2").doesNotExist())
        .andExpect(jsonPath("$.data.animals[0].triggerPercentage").doesNotExist())
        .andExpect(jsonPath("$.data.animals[0].point").doesNotExist());
  }

  @Test
  void startsBattleAndReturnsBarRangeWithRests() throws Exception {
    given(battleStartService.start(eq(USER_ID), any(), any()))
        .willReturn(
            new BattleStartResponse(
                101L,
                4L,
                BattlePolicy.INITIAL_BAR_POSITION,
                BattlePolicy.MIN_BAR_POSITION,
                BattlePolicy.MAX_BAR_POSITION,
                new BattleUserEntryResponse(
                    31L,
                    1,
                    "솜구름",
                    CardType.SKY,
                    Tier.A,
                    new BattleSkillResponse("깃털 방어", SkillBattleType.STABLE, 80, 1),
                    new BattleSkillResponse("순풍 타기", SkillBattleType.BALANCED, 45, 2)),
                new BattleNpcEntryResponse(1, "별콩", CardType.SPACE, Tier.B, null),
                List.of(new BattleRestResponse(31L, Instant.parse("2026-08-25T05:30:00Z")))));

    mockMvc
        .perform(
            post("/api/battles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "gymLeaderId": 4,
                      "clientRequestId": "b3f1c2a0-5d6e-4f7a-8b9c-0d1e2f3a4b5c",
                      "entries": [
                        {"animalId": 31, "orderNo": 1},
                        {"animalId": 32, "orderNo": 2},
                        {"animalId": 33, "orderNo": 3}
                      ]
                    }
                    """)
                .with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.battleId").value(101))
        .andExpect(jsonPath("$.data.barPosition").value(0))
        .andExpect(jsonPath("$.data.minBarPosition").value(-15))
        .andExpect(jsonPath("$.data.maxBarPosition").value(15))
        .andExpect(jsonPath("$.data.userEntry.skill1.point").value(1))
        .andExpect(jsonPath("$.data.npcEntry.skill1").doesNotExist())
        .andExpect(jsonPath("$.data.rests[0].restEndsAt").exists());
  }

  @Test
  void rejectsLockedGymLeaderWithForbidden() throws Exception {
    given(battleStartService.start(eq(USER_ID), any(), any()))
        .willThrow(new BusinessException(ErrorCode.GYM_LEADER_LOCKED));

    mockMvc
        .perform(
            post("/api/battles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "gymLeaderId": 4,
                      "clientRequestId": "b3f1c2a0-5d6e-4f7a-8b9c-0d1e2f3a4b5c",
                      "entries": [
                        {"animalId": 31, "orderNo": 1},
                        {"animalId": 32, "orderNo": 2},
                        {"animalId": 33, "orderNo": 3}
                      ]
                    }
                    """)
                .with(authentication(userAuthentication())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("GYM_LEADER_LOCKED"));
  }

  private static GymLeaderProfileResponse gymLeaderProfileResponse(
      boolean unlocked, boolean levelSatisfied, String previousBadgeCode, boolean badgeSatisfied) {
    return new GymLeaderProfileResponse(
        4L,
        "GYM004",
        "노바",
        4,
        "https://cdn/image.png",
        "BDG009",
        false,
        new GymLeaderUnlockResponse(
            unlocked, 12, levelSatisfied, previousBadgeCode, badgeSatisfied));
  }

  private static Authentication userAuthentication() {
    return new JwtAuthenticationToken("token", new UserPrincipal(USER_ID), null);
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-08-25T05:00:00Z"), ZoneOffset.UTC);
    }

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
