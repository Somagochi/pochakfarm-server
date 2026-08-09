package com.somagochi.pochakfarm.capture.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.capture.application.CaptureAnimalService;
import com.somagochi.pochakfarm.capture.application.CaptureAttemptPurchaseService;
import com.somagochi.pochakfarm.capture.application.CaptureAvailabilityService;
import com.somagochi.pochakfarm.capture.application.CaptureCompleteService;
import com.somagochi.pochakfarm.capture.application.CaptureGameResultService;
import com.somagochi.pochakfarm.capture.application.CaptureOverviewService;
import com.somagochi.pochakfarm.capture.application.CaptureQueryService;
import com.somagochi.pochakfarm.capture.application.CaptureStartService;
import com.somagochi.pochakfarm.capture.domain.CaptureDifficulty;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.domain.TierProbability;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureAttemptPurchaseRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAttemptPurchaseResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureCompleteResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse.Progression;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse.ProgressionState;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse.Reward;
import com.somagochi.pochakfarm.capture.dto.CaptureOverviewResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse.Attempts;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse.Upload;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.config.SecurityConfig;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.exception.GlobalExceptionHandler;
import com.somagochi.pochakfarm.common.jwt.JwtPayload;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CaptureController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  CaptureControllerTest.TestConfig.class
})
class CaptureControllerTest {

  private static final String CLIENT_REQUEST_ID = "550e8400-e29b-41d4-a716-446655440000";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CaptureStartService captureStartService;
  @MockitoBean private CaptureAttemptPurchaseService captureAttemptPurchaseService;
  @MockitoBean private CaptureAnimalService captureAnimalService;
  @MockitoBean private CaptureAvailabilityService captureAvailabilityService;
  @MockitoBean private CaptureCompleteService captureCompleteService;
  @MockitoBean private CaptureGameResultService captureGameResultService;
  @MockitoBean private CaptureOverviewService captureOverviewService;
  @MockitoBean private CaptureQueryService captureQueryService;

  @Test
  void startsCaptureWhenAuthenticated() throws Exception {
    Instant expiresAt = Instant.parse("2026-07-24T01:05:00Z");
    given(
            captureStartService.startCapture(
                1L, new CaptureStartRequest(CLIENT_REQUEST_ID, "image/jpeg", "두부")))
        .willReturn(
            new CaptureStartResponse(
                123L,
                Tier.B,
                CardType.GROUND,
                new CaptureDifficulty(2_800),
                new Upload(
                    "https://upload.example/original",
                    "images/capture-original/1/original.jpg",
                    expiresAt),
                new Attempts(4),
                expiresAt));

    mockMvc
        .perform(
            post("/api/captures")
                .with(authentication(authenticationFor(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "clientRequestId": "%s",
                      "contentType": "image/jpeg",
                      "animalName": "두부"
                    }
                    """
                        .formatted(CLIENT_REQUEST_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.captureId").value(123))
        .andExpect(jsonPath("$.data.tier").value("B"))
        .andExpect(jsonPath("$.data.cardType").value("GROUND"))
        .andExpect(jsonPath("$.data.difficulty.ringShrinkDurationMs").value(2800))
        .andExpect(jsonPath("$.data.difficulty.roundDurationMs").doesNotExist())
        .andExpect(jsonPath("$.data.difficulty.maxThrows").doesNotExist())
        .andExpect(jsonPath("$.data.difficulty.successWindowMs").doesNotExist())
        .andExpect(jsonPath("$.data.upload.url").value("https://upload.example/original"))
        .andExpect(jsonPath("$.data.upload.key").value("images/capture-original/1/original.jpg"))
        .andExpect(jsonPath("$.data.attempts.remaining").value(4))
        .andExpect(jsonPath("$.data.gameResultExpiresAt").value("2026-07-24T01:05:00Z"));

    verify(captureStartService)
        .startCapture(1L, new CaptureStartRequest(CLIENT_REQUEST_ID, "image/jpeg", "두부"));
  }

  @Test
  void returnsUnauthorizedWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/captures")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "clientRequestId": "%s",
                      "contentType": "image/jpeg",
                      "animalName": "두부"
                    }
                    """
                        .formatted(CLIENT_REQUEST_ID)))
        .andExpect(status().isUnauthorized());

    verify(captureStartService, org.mockito.Mockito.never()).startCapture(any(), any());
  }

  @Test
  void returnsAttemptRequiredWhenAttemptsAreExhausted() throws Exception {
    given(
            captureStartService.startCapture(
                1L, new CaptureStartRequest(CLIENT_REQUEST_ID, "image/jpeg", "두부")))
        .willThrow(new BusinessException(ErrorCode.CAPTURE_ATTEMPT_REQUIRED));

    mockMvc
        .perform(
            post("/api/captures")
                .with(authentication(authenticationFor(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "clientRequestId": "%s",
                      "contentType": "image/jpeg",
                      "animalName": "두부"
                    }
                    """
                        .formatted(CLIENT_REQUEST_ID)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CAPTURE_ATTEMPT_REQUIRED"));
  }

  @Test
  void returnsCaptureAvailability() throws Exception {
    given(captureAvailabilityService.getAvailability(1L))
        .willReturn(
            new CaptureAvailabilityResponse(
                new CaptureAvailabilityResponse.Attempts(0, Instant.parse("2026-08-02T15:00:00Z")),
                200,
                1000));

    mockMvc
        .perform(get("/api/captures/availability").with(authentication(authenticationFor(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.attempts.remaining").value(0))
        .andExpect(jsonPath("$.data.attempts.resetsAt").value("2026-08-02T15:00:00Z"))
        .andExpect(jsonPath("$.data.attemptPurchaseCost").value(200))
        .andExpect(jsonPath("$.data.coins").value(1000));
  }

  @Test
  void purchasesCaptureAttempt() throws Exception {
    given(
            captureAttemptPurchaseService.purchase(
                1L, new CaptureAttemptPurchaseRequest(CLIENT_REQUEST_ID)))
        .willReturn(
            new CaptureAttemptPurchaseResponse(1, 200, 800, Instant.parse("2026-08-02T15:00:00Z")));

    mockMvc
        .perform(
            post("/api/captures/attempts/purchase")
                .with(authentication(authenticationFor(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientRequestId":"%s"}
                    """
                        .formatted(CLIENT_REQUEST_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.remaining").value(1))
        .andExpect(jsonPath("$.data.chargedCoins").value(200))
        .andExpect(jsonPath("$.data.currentCoins").value(800))
        .andExpect(jsonPath("$.data.resetsAt").value("2026-08-02T15:00:00Z"));
  }

  @Test
  void returnsCaptureOverview() throws Exception {
    given(captureOverviewService.getOverview(1L))
        .willReturn(
            new CaptureOverviewResponse(
                new CaptureOverviewResponse.Level(12, 54, 150, 96),
                List.of(
                    new CaptureOverviewResponse.CaptureCount(CardType.SKY, 23),
                    new CaptureOverviewResponse.CaptureCount(CardType.GROUND, 47),
                    new CaptureOverviewResponse.CaptureCount(CardType.SEA, 0),
                    new CaptureOverviewResponse.CaptureCount(CardType.SPACE, 1)),
                List.of(
                    new TierProbability(Tier.C, new BigDecimal("44.9")),
                    new TierProbability(Tier.B, new BigDecimal("38")))));

    mockMvc
        .perform(get("/api/captures/overview").with(authentication(authenticationFor(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.level.currentLevel").value(12))
        .andExpect(jsonPath("$.data.level.currentExperience").value(54))
        .andExpect(jsonPath("$.data.level.requiredExperience").value(150))
        .andExpect(jsonPath("$.data.level.remainingExperience").value(96))
        .andExpect(jsonPath("$.data.captureCounts[0].cardType").value("SKY"))
        .andExpect(jsonPath("$.data.captureCounts[0].count").value(23))
        .andExpect(jsonPath("$.data.captureCounts[1].cardType").value("GROUND"))
        .andExpect(jsonPath("$.data.captureCounts[2].cardType").value("SEA"))
        .andExpect(jsonPath("$.data.captureCounts[2].count").value(0))
        .andExpect(jsonPath("$.data.captureCounts[3].cardType").value("SPACE"))
        .andExpect(jsonPath("$.data.tierProbabilities[0].tier").value("C"))
        .andExpect(jsonPath("$.data.tierProbabilities[0].probabilityPercent").value(44.9))
        .andExpect(jsonPath("$.data.tierProbabilities[1].probabilityPercent").value(38));

    verify(captureOverviewService).getOverview(1L);
  }

  @Test
  void rejectsUnauthenticatedCaptureOverview() throws Exception {
    mockMvc.perform(get("/api/captures/overview")).andExpect(status().isUnauthorized());

    verify(captureOverviewService, org.mockito.Mockito.never()).getOverview(any());
  }

  @Test
  void completesOriginalImageWithAcceptedStatus() throws Exception {
    given(captureCompleteService.completeOriginalImage(1L, 123L))
        .willReturn(new CaptureCompleteResponse(123L, GenerationStatus.PROCESSING));

    mockMvc
        .perform(
            post("/api/captures/123/original-image/complete")
                .with(authentication(authenticationFor(1L))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.captureId").value(123))
        .andExpect(jsonPath("$.data.generationStatus").value("PROCESSING"));
  }

  @Test
  void returnsCaptureStatus() throws Exception {
    given(captureQueryService.getCapture(1L, 123L))
        .willReturn(
            new CaptureResponse(
                123L,
                Tier.S,
                CardType.GROUND,
                GenerationStatus.SUCCEEDED,
                GameStatus.PENDING,
                "https://cdn.test/public/capture-card/card.png",
                "https://cdn.test/public/capture-animal/animal.png",
                18420,
                null));

    mockMvc
        .perform(get("/api/captures/123").with(authentication(authenticationFor(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.captureId").value(123))
        .andExpect(jsonPath("$.data.generationStatus").value("SUCCEEDED"))
        .andExpect(
            jsonPath("$.data.cardImageUrl").value("https://cdn.test/public/capture-card/card.png"))
        .andExpect(
            jsonPath("$.data.animalImageUrl")
                .value("https://cdn.test/public/capture-animal/animal.png"));
  }

  @Test
  void savesCapturedAnimalAtSelectedSlot() throws Exception {
    CaptureAnimalPlacementRequest request = new CaptureAnimalPlacementRequest(1, 2, null);
    given(captureAnimalService.place(1L, 123L, request))
        .willReturn(
            new CaptureAnimalPlacementResponse(
                10L,
                123L,
                CardType.GROUND,
                1,
                2,
                "https://cdn.test/public/capture-animal/1/123.png"));

    mockMvc
        .perform(
            post("/api/captures/123/animal")
                .with(authentication(authenticationFor(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "floorNum": 1,
                      "slotNum": 2
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.animalId").value(10))
        .andExpect(jsonPath("$.data.floorNum").value(1))
        .andExpect(jsonPath("$.data.slotNum").value(2));

    verify(captureAnimalService).place(1L, 123L, request);
  }

  @Test
  void submitsGameResult() throws Exception {
    CaptureGameResultRequest request =
        new CaptureGameResultRequest(
            java.util.List.of(
                new CaptureGameResultRequest.ThrowResult(1, false),
                new CaptureGameResultRequest.ThrowResult(2, true)));
    given(captureGameResultService.submit(1L, 123L, request))
        .willReturn(
            new CaptureGameResultResponse(
                123L,
                GameStatus.SUCCEEDED,
                new Reward(15, 500L),
                new Progression(new ProgressionState(2, 45, 50), new ProgressionState(3, 10, 60))));

    mockMvc
        .perform(
            post("/api/captures/123/game-result")
                .with(authentication(authenticationFor(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "throws": [
                        {"round": 1, "succeeded": false},
                        {"round": 2, "succeeded": true}
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.captureId").value(123))
        .andExpect(jsonPath("$.data.gameStatus").value("SUCCEEDED"))
        .andExpect(jsonPath("$.data.reward.experienceReward").value(15))
        .andExpect(jsonPath("$.data.reward.levelUpCoinReward").value(500))
        .andExpect(jsonPath("$.data.progression.before.level").value(2))
        .andExpect(jsonPath("$.data.progression.before.experience").value(45))
        .andExpect(jsonPath("$.data.progression.after.level").value(3))
        .andExpect(jsonPath("$.data.progression.after.experience").value(10))
        .andExpect(jsonPath("$.data.progression.after.requiredExperienceForNextLevel").value(60));

    verify(captureGameResultService).submit(1L, 123L, request);
  }

  @Test
  void returnsBadRequestForInvalidGameResult() throws Exception {
    CaptureGameResultRequest request =
        new CaptureGameResultRequest(
            java.util.List.of(new CaptureGameResultRequest.ThrowResult(1, false)));
    given(captureGameResultService.submit(1L, 123L, request))
        .willThrow(new BusinessException(ErrorCode.INVALID_GAME_RESULT));

    mockMvc
        .perform(
            post("/api/captures/123/game-result")
                .with(authentication(authenticationFor(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "throws": [
                        {"round": 1, "succeeded": false}
                      ]
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_GAME_RESULT"));
  }

  private JwtAuthenticationToken authenticationFor(long userId) {
    return new JwtAuthenticationToken(
        "access-token",
        new UserPrincipal(userId),
        new JwtPayload(
            "jti-1",
            String.valueOf(userId),
            Instant.parse("2026-07-24T00:00:00Z"),
            Instant.parse("2026-07-24T02:00:00Z"),
            Map.of("jti", "jti-1", "tokenType", "access")));
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
