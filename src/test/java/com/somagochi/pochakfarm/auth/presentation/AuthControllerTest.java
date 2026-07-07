package com.somagochi.pochakfarm.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.auth.application.LogoutService;
import com.somagochi.pochakfarm.auth.application.RefreshService;
import com.somagochi.pochakfarm.auth.application.SocialLoginService;
import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.config.SecurityConfig;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.exception.GlobalExceptionHandler;
import com.somagochi.pochakfarm.common.jwt.JwtPayload;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationException;
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

@WebMvcTest(AuthController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  AuthControllerTest.TestConfig.class
})
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private SocialLoginService socialLoginService;
  @MockitoBean private LogoutService logoutService;
  @MockitoBean private RefreshService refreshService;

  @Test
  void returnsTokenPairWhenLoginSucceeds() throws Exception {
    given(socialLoginService.login(any(SocialLoginRequest.class)))
        .willReturn(
            new SocialLoginResponse(new TokenResponse("access-token", "refresh-token"), true));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"kakao\",\"token\":\"social-token\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token.accessToken").value("access-token"))
        .andExpect(jsonPath("$.data.token.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.data.isNew").value(true));
  }

  @Test
  void mapsBusinessExceptionToErrorResponse() throws Exception {
    given(socialLoginService.login(any(SocialLoginRequest.class)))
        .willThrow(new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"unknown\",\"token\":\"social-token\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_SOCIAL_PROVIDER"));
  }

  @Test
  void refreshesTokenPairWithoutAuthentication() throws Exception {
    given(refreshService.refresh("refresh-token"))
        .willReturn(new TokenResponse("new-access-token", "new-refresh-token"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-token\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
        .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));

    verify(refreshService).refresh("refresh-token");
  }

  @Test
  void mapsRevokedRefreshTokenToErrorResponse() throws Exception {
    given(refreshService.refresh("refresh-token"))
        .willThrow(new JwtAuthenticationException(ErrorCode.REVOKED_REFRESH_TOKEN));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-token\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("REVOKED_REFRESH_TOKEN"));
  }

  @Test
  void logsOutWhenAuthenticated() throws Exception {
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            "access-token",
            new UserPrincipal(1L),
            new JwtPayload(
                "jti-1",
                "1",
                Instant.parse("2026-05-26T00:00:00Z"),
                Instant.parse("2026-05-26T01:00:00Z"),
                Map.of("jti", "jti-1", "tokenType", "access")));

    mockMvc
        .perform(
            post("/api/auth/logout")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-token\"}"))
        .andExpect(status().isOk());

    verify(logoutService).logout("access-token", "refresh-token");
  }

  @Test
  void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
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
