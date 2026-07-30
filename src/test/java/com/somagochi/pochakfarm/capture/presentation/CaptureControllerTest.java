package com.somagochi.pochakfarm.capture.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.capture.application.CaptureStartService;
import com.somagochi.pochakfarm.capture.domain.CaptureDifficulty;
import com.somagochi.pochakfarm.capture.domain.Tier;
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
                new CaptureDifficulty(10_000, 3, 2_800, 280),
                new Upload(
                    "https://upload.example/original",
                    "images/capture-original/1/original.jpg",
                    expiresAt),
                new Attempts(5, 1, 4),
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
        .andExpect(jsonPath("$.data.difficulty.roundDurationMs").value(10000))
        .andExpect(jsonPath("$.data.difficulty.maxThrows").value(3))
        .andExpect(jsonPath("$.data.difficulty.ringShrinkDurationMs").value(2800))
        .andExpect(jsonPath("$.data.difficulty.successWindowMs").value(280))
        .andExpect(jsonPath("$.data.upload.url").value("https://upload.example/original"))
        .andExpect(jsonPath("$.data.upload.key").value("images/capture-original/1/original.jpg"))
        .andExpect(jsonPath("$.data.attempts.dailyLimit").value(5))
        .andExpect(jsonPath("$.data.attempts.used").value(1))
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
  void returnsConflictWhenDailyAttemptsAreExhausted() throws Exception {
    given(
            captureStartService.startCapture(
                1L, new CaptureStartRequest(CLIENT_REQUEST_ID, "image/jpeg", "두부")))
        .willThrow(new BusinessException(ErrorCode.CAPTURE_ATTEMPT_EXHAUSTED));

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
        .andExpect(jsonPath("$.code").value("CAPTURE_ATTEMPT_EXHAUSTED"));
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
