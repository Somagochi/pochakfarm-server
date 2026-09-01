package com.somagochi.pochakfarm.battle.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.battle.application.BattleActionService;
import com.somagochi.pochakfarm.battle.application.BattleFinalRoundService;
import com.somagochi.pochakfarm.battle.application.BattleStateQueryService;
import com.somagochi.pochakfarm.battle.domain.BattleEventCode;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import com.somagochi.pochakfarm.battle.domain.SkillActivationStatus;
import com.somagochi.pochakfarm.battle.dto.BattleActionRequest;
import com.somagochi.pochakfarm.battle.dto.BattleActionResponse;
import com.somagochi.pochakfarm.battle.dto.BattleBroadcastEventResponse;
import com.somagochi.pochakfarm.battle.dto.BattleEntryResponse;
import com.somagochi.pochakfarm.battle.dto.BattleEntrySkillResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultRequest;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundStartResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundStateResponse;
import com.somagochi.pochakfarm.battle.dto.BattleSkillOutcomeResponse;
import com.somagochi.pochakfarm.battle.dto.BattleStateResponse;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
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
import java.time.Instant;
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

@WebMvcTest(BattleActionController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  BattleActionControllerTest.TestConfig.class
})
class BattleActionControllerTest {

  private static final Long USER_ID = 1L;
  private static final Long BATTLE_ID = 7L;
  private static final Instant EXPIRES_AT = Instant.parse("2026-08-25T00:00:03Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BattleActionService battleActionService;

  @MockitoBean private BattleStateQueryService battleStateQueryService;

  @MockitoBean private BattleFinalRoundService battleFinalRoundService;

  @Test
  void returnsResolutionOfSelectedSkill() throws Exception {
    given(
            battleActionService.selectSkill(
                USER_ID, BATTLE_ID, new BattleActionRequest(1, CardSkill.SEA_WAVE_DASH)))
        .willReturn(actionResponse());

    mockMvc
        .perform(
            post("/api/battles/{battleId}/actions", BATTLE_ID)
                .with(authentication(userAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "actionSeq": 1,
                      "skill": "SEA_WAVE_DASH"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.actionSeq").value(1))
        .andExpect(jsonPath("$.data.user.skill").value("SEA_WAVE_DASH"))
        .andExpect(jsonPath("$.data.user.status").value("ACTIVATED"))
        .andExpect(jsonPath("$.data.user.point").value(3))
        .andExpect(jsonPath("$.data.npc.status").value("FAILED"))
        .andExpect(jsonPath("$.data.netPoint").value(3))
        .andExpect(jsonPath("$.data.barPosition").value(3))
        .andExpect(jsonPath("$.data.minBarPosition").value(BattlePolicy.MIN_BAR_POSITION))
        .andExpect(jsonPath("$.data.maxBarPosition").value(BattlePolicy.MAX_BAR_POSITION))
        .andExpect(jsonPath("$.data.nextActionSeq").value(2))
        .andExpect(jsonPath("$.data.broadcastEvents.length()").value(1))
        .andExpect(jsonPath("$.data.broadcastEvents[0].point").value(3));
  }

  @Test
  void acceptsNoSelectionAsNullSkill() throws Exception {
    given(battleActionService.selectSkill(USER_ID, BATTLE_ID, new BattleActionRequest(1, null)))
        .willReturn(notSelectedActionResponse());

    mockMvc
        .perform(
            post("/api/battles/{battleId}/actions", BATTLE_ID)
                .with(authentication(userAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "actionSeq": 1
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.skill").isEmpty())
        .andExpect(jsonPath("$.data.user.status").value("NOT_SELECTED"))
        .andExpect(jsonPath("$.data.user.point").value(0));
  }

  @Test
  void returnsConflictWhenActionSeqDoesNotMatch() throws Exception {
    given(
            battleActionService.selectSkill(
                USER_ID, BATTLE_ID, new BattleActionRequest(5, CardSkill.SEA_WAVE_DASH)))
        .willThrow(new BusinessException(ErrorCode.BATTLE_ACTION_SEQUENCE_MISMATCH));

    mockMvc
        .perform(
            post("/api/battles/{battleId}/actions", BATTLE_ID)
                .with(authentication(userAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "actionSeq": 5,
                      "skill": "SEA_WAVE_DASH"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.BATTLE_ACTION_SEQUENCE_MISMATCH.getCode()));
  }

  @Test
  void returnsBattleStateForReconnection() throws Exception {
    given(battleStateQueryService.getBattle(USER_ID, BATTLE_ID)).willReturn(stateResponse());

    mockMvc
        .perform(
            get("/api/battles/{battleId}", BATTLE_ID).with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.battleId").value(BATTLE_ID))
        .andExpect(jsonPath("$.data.barPosition").value(4))
        .andExpect(jsonPath("$.data.completedActionCount").value(4))
        .andExpect(jsonPath("$.data.currentEntryOrder").value(2))
        .andExpect(jsonPath("$.data.nextActionSeq").value(5))
        .andExpect(jsonPath("$.data.userEntry.skills.length()").value(1))
        .andExpect(jsonPath("$.data.npcEntry.skills").isEmpty())
        .andExpect(jsonPath("$.data.broadcastEvents.length()").value(1));
  }

  @Test
  void returnsNotFoundWhenBattleDoesNotExist() throws Exception {
    given(battleStateQueryService.getBattle(USER_ID, BATTLE_ID))
        .willThrow(new BusinessException(ErrorCode.BATTLE_NOT_FOUND));

    mockMvc
        .perform(
            get("/api/battles/{battleId}", BATTLE_ID).with(authentication(userAuthentication())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorCode.BATTLE_NOT_FOUND.getCode()));
  }

  @Test
  void startsFinalRoundAfterClientAnimationIsReady() throws Exception {
    BattleFinalRoundStateResponse finalRound =
        new BattleFinalRoundStateResponse(
            true,
            true,
            EXPIRES_AT.plusSeconds(27),
            EXPIRES_AT,
            EXPIRES_AT.plusSeconds(1),
            null,
            null);
    given(battleFinalRoundService.start(USER_ID, BATTLE_ID))
        .willReturn(
            new BattleFinalRoundStartResponse(
                BATTLE_ID, BattleStatus.IN_PROGRESS, null, finalRound, null));

    mockMvc
        .perform(
            post("/api/battles/{battleId}/final-round/start", BATTLE_ID)
                .with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.finalRound.required").value(true))
        .andExpect(jsonPath("$.data.finalRound.started").value(true))
        .andExpect(jsonPath("$.data.finalRound.inputExpiresAt").exists())
        .andExpect(jsonPath("$.data.finalRound.submissionExpiresAt").exists());
  }

  @Test
  void submitsTapCountAndReturnsFinalResult() throws Exception {
    BattleFinalRoundStateResponse finalRound =
        new BattleFinalRoundStateResponse(
            true, true, EXPIRES_AT.plusSeconds(27), EXPIRES_AT, EXPIRES_AT.plusSeconds(1), 20, 3);
    given(battleFinalRoundService.submit(USER_ID, BATTLE_ID, new BattleFinalRoundResultRequest(20)))
        .willReturn(
            new BattleFinalRoundResultResponse(
                BATTLE_ID, BattleStatus.FINISHED, BattleResult.WIN, 1, finalRound, null));

    mockMvc
        .perform(
            post("/api/battles/{battleId}/final-round/result", BATTLE_ID)
                .with(authentication(userAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tapCount\":20}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.battleResult").value("WIN"))
        .andExpect(jsonPath("$.data.finalRound.tapCount").value(20))
        .andExpect(jsonPath("$.data.finalRound.point").value(3));
  }

  @Test
  void returnsUnauthorizedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/battles/{battleId}", BATTLE_ID)).andExpect(status().isUnauthorized());
  }

  private static BattleActionResponse actionResponse() {
    return new BattleActionResponse(
        BATTLE_ID,
        1,
        1,
        1,
        new BattleSkillOutcomeResponse(
            CardSkill.SEA_WAVE_DASH,
            CardSkill.SEA_WAVE_DASH.displayName(),
            SkillBattleType.GAMBLE,
            SkillActivationStatus.ACTIVATED,
            3),
        new BattleSkillOutcomeResponse(
            CardSkill.SEA_SEASHELL_SHIELD,
            CardSkill.SEA_SEASHELL_SHIELD.displayName(),
            SkillBattleType.STABLE,
            SkillActivationStatus.FAILED,
            0),
        3,
        3,
        BattlePolicy.MIN_BAR_POSITION,
        BattlePolicy.MAX_BAR_POSITION,
        BattleStatus.IN_PROGRESS,
        null,
        2,
        EXPIRES_AT,
        noFinalRound(),
        null,
        List.of(battlePointAppliedEvent()));
  }

  private static BattleActionResponse notSelectedActionResponse() {
    return new BattleActionResponse(
        BATTLE_ID,
        1,
        1,
        1,
        new BattleSkillOutcomeResponse(null, null, null, SkillActivationStatus.NOT_SELECTED, 0),
        new BattleSkillOutcomeResponse(
            CardSkill.SEA_SEASHELL_SHIELD,
            CardSkill.SEA_SEASHELL_SHIELD.displayName(),
            SkillBattleType.STABLE,
            SkillActivationStatus.FAILED,
            0),
        0,
        0,
        BattlePolicy.MIN_BAR_POSITION,
        BattlePolicy.MAX_BAR_POSITION,
        BattleStatus.IN_PROGRESS,
        null,
        2,
        EXPIRES_AT,
        noFinalRound(),
        null,
        List.of());
  }

  private static BattleStateResponse stateResponse() {
    return new BattleStateResponse(
        BATTLE_ID,
        3L,
        BattleStatus.IN_PROGRESS,
        null,
        4,
        BattlePolicy.MIN_BAR_POSITION,
        BattlePolicy.MAX_BAR_POSITION,
        4,
        BattlePolicy.TOTAL_ACTION_COUNT,
        2,
        5,
        EXPIRES_AT,
        new BattleEntryResponse(
            BattleSide.USER,
            2,
            10L,
            "유저2",
            CardType.SEA,
            Tier.A,
            List.of(
                new BattleEntrySkillResponse(
                    CardSkill.SEA_WAVE_DASH,
                    CardSkill.SEA_WAVE_DASH.displayName(),
                    SkillBattleType.GAMBLE,
                    30,
                    3))),
        new BattleEntryResponse(BattleSide.NPC, 2, null, "관장2", CardType.SPACE, Tier.B, null),
        noFinalRound(),
        null,
        List.of(battlePointAppliedEvent()));
  }

  private static BattleFinalRoundStateResponse noFinalRound() {
    return new BattleFinalRoundStateResponse(false, false, null, null, null, null, null);
  }

  private static BattleBroadcastEventResponse battlePointAppliedEvent() {
    return new BattleBroadcastEventResponse(
        1, 1, 1, BattleEventCode.BATTLE_POINT_APPLIED, null, null, null, BattleSide.USER, 3);
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
