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
import com.somagochi.pochakfarm.capture.application.CaptureAvailabilityService;
import com.somagochi.pochakfarm.capture.application.CaptureCompleteService;
import com.somagochi.pochakfarm.capture.application.CaptureGameResultService;
import com.somagochi.pochakfarm.capture.application.CaptureQueryService;
import com.somagochi.pochakfarm.capture.application.CaptureStartService;
import com.somagochi.pochakfarm.capture.domain.CaptureDifficulty;
import com.somagochi.pochakfarm.capture.domain.CapturePaymentType;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse.FreeAttempts;
import com.somagochi.pochakfarm.capture.dto.CaptureCompleteResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse.Progression;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse.ProgressionState;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse.Reward;
import com.somagochi.pochakfarm.capture.dto.CaptureResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse.Attempts;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse.Payment;
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
import java.time.Instant;
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
  @MockitoBean private CaptureAnimalService captureAnimalService;
  @MockitoBean private CaptureAvailabilityService captureAvailabilityService;
  @MockitoBean private CaptureCompleteService captureCompleteService;
  @MockitoBean private CaptureGameResultService captureGameResultService;
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
                new Attempts(5, 1, 4),
                new Payment(CapturePaymentType.FREE, 0, 1000),
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
        .andExpect(jsonPath("$.data.attempts.dailyLimit").value(5))
        .andExpect(jsonPath("$.data.attempts.used").value(1))
        .andExpect(jsonPath("$.data.attempts.remaining").value(4))
        .andExpect(jsonPath("$.data.payment.type").value("FREE"))
        .andExpect(jsonPath("$.data.payment.chargedCoins").value(0))
        .andExpect(jsonPath("$.data.payment.currentCoins").value(1000))
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
  void returnsPaymentRequiredWhenFreeAttemptsAreExhaustedWithoutConsent() throws Exception {
    given(
            captureStartService.startCapture(
                1L, new CaptureStartRequest(CLIENT_REQUEST_ID, "image/jpeg", "두부")))
        .willThrow(new BusinessException(ErrorCode.COIN_PAYMENT_REQUIRED));

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
        .andExpect(status().isPaymentRequired())
        .andExpect(jsonPath("$.code").value("COIN_PAYMENT_REQUIRED"));
  }

  @Test
  void returnsCaptureAvailability() throws Exception {
    given(captureAvailabilityService.getAvailability(1L))
        .willReturn(
            new CaptureAvailabilityResponse(
                new FreeAttempts(5, 5, 0, Instant.parse("2026-08-02T15:00:00Z")), 200, 1000, true));

    mockMvc
        .perform(get("/api/captures/availability").with(authentication(authenticationFor(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.freeAttempts.dailyLimit").value(5))
        .andExpect(jsonPath("$.data.freeAttempts.used").value(5))
        .andExpect(jsonPath("$.data.freeAttempts.remaining").value(0))
        .andExpect(jsonPath("$.data.freeAttempts.resetsAt").value("2026-08-02T15:00:00Z"))
        .andExpect(jsonPath("$.data.extraCaptureCost").value(200))
        .andExpect(jsonPath("$.data.coins").value(1000))
        .andExpect(jsonPath("$.data.canStartCapture").value(true));
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
                "https://cdn.test/public/capture-scene/scene.png",
                "https://cdn.test/public/capture-card/card.png",
                null,
                18420,
                null));

    mockMvc
        .perform(get("/api/captures/123").with(authentication(authenticationFor(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.captureId").value(123))
        .andExpect(jsonPath("$.data.generationStatus").value("SUCCEEDED"))
        .andExpect(
            jsonPath("$.data.sceneImageUrl")
                .value("https://cdn.test/public/capture-scene/scene.png"))
        .andExpect(
            jsonPath("$.data.cardImageUrl").value("https://cdn.test/public/capture-card/card.png"));
  }

  @Test
  void presignsCaptureAnimalImageUpload() throws Exception {
    Instant expiresAt = Instant.parse("2026-08-03T01:05:00Z");
    given(captureAnimalService.presign(1L, 123L))
        .willReturn(
            new com.somagochi.pochakfarm.storage.dto.PresignResponse(
                "https://upload.test/animal", "public/capture-animal/1/123.png", expiresAt));

    mockMvc
        .perform(
            post("/api/captures/123/animal-image/presign")
                .with(authentication(authenticationFor(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.uploadUrl").value("https://upload.test/animal"))
        .andExpect(jsonPath("$.data.key").value("public/capture-animal/1/123.png"));
  }

  @Test
  void savesCapturedAnimalAtSelectedSlot() throws Exception {
    CaptureAnimalPlacementRequest request =
        new CaptureAnimalPlacementRequest("public/capture-animal/1/123.png", 1, 2, null);
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
                      "animalImageKey": "public/capture-animal/1/123.png",
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
