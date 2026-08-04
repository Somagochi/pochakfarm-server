package com.somagochi.pochakfarm.user.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.application.UserNicknameService;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import com.somagochi.pochakfarm.user.application.UserTermsAgreementService;
import com.somagochi.pochakfarm.user.application.WithdrawService;
import com.somagochi.pochakfarm.user.dto.NicknameResponse;
import com.somagochi.pochakfarm.user.dto.UserProfileResponse;
import com.somagochi.pochakfarm.user.dto.UserResponse;
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

@WebMvcTest(UserController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  UserControllerTest.TestConfig.class
})
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserQueryService userQueryService;
  @MockitoBean private UserNicknameService userNicknameService;
  @MockitoBean private UserTermsAgreementService userTermsAgreementService;
  @MockitoBean private WithdrawService withdrawService;

  @Test
  void returnsCurrentUserWhenAuthenticated() throws Exception {
    given(userQueryService.getMe(1L))
        .willReturn(new UserResponse("user@example.com", SocialProvider.KAKAO, "포착이"));

    mockMvc
        .perform(get("/api/users/me").with(authentication(authenticationFor(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value("user@example.com"))
        .andExpect(jsonPath("$.data.provider").value("KAKAO"))
        .andExpect(jsonPath("$.data.nickname").value("포착이"));
  }

  @Test
  void returnsProfileWhenAuthenticated() throws Exception {
    given(userQueryService.getProfile(1L)).willReturn(new UserProfileResponse("포착이", 3, 1200L));

    mockMvc
        .perform(get("/api/users/profile").with(authentication(authenticationFor(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nickname").value("포착이"))
        .andExpect(jsonPath("$.data.level").value(3))
        .andExpect(jsonPath("$.data.coins").value(1200));
  }

  @Test
  void returnsUnauthorizedWhenGettingProfileWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/users/profile")).andExpect(status().isUnauthorized());
  }

  @Test
  void mapsBusinessExceptionToErrorResponse() throws Exception {
    given(userQueryService.getMe(1L)).willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

    mockMvc
        .perform(get("/api/users/me").with(authentication(authenticationFor(1L))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void changesNicknameWhenAuthenticated() throws Exception {
    given(userNicknameService.changeNickname(1L, "포착이")).willReturn(new NicknameResponse("포착이"));

    mockMvc
        .perform(
            patch("/api/users/me/nickname")
                .with(authentication(authenticationFor(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"포착이\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nickname").value("포착이"));

    verify(userNicknameService).changeNickname(1L, "포착이");
  }

  @Test
  void returnsUnauthorizedWhenChangingNicknameWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/me/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"포착이\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void agreesToTermsWhenAuthenticated() throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/terms-agreement")
                .with(authentication(authenticationFor(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "ageRequirementAgreed": true,
                      "termsOfServiceAgreed": true,
                      "privacyPolicyAgreed": true,
                      "serviceQualityAgreed": false,
                      "marketingAgreed": true
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").doesNotExist());

    verify(userTermsAgreementService)
        .agree(
            1L,
            new com.somagochi.pochakfarm.user.dto.TermsAgreementRequest(
                true, true, true, false, true));
  }

  @Test
  void returnsUnauthorizedWhenAgreeingToTermsWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/terms-agreement")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void withdrawsWhenAuthenticated() throws Exception {
    mockMvc
        .perform(
            delete("/api/users/me")
                .with(authentication(authenticationFor(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-token\"}"))
        .andExpect(status().isOk());

    verify(withdrawService).withdraw(1L, "access-token", "refresh-token");
  }

  @Test
  void returnsUnauthorizedWhenWithdrawingWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            delete("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-token\"}"))
        .andExpect(status().isUnauthorized());
  }

  private JwtAuthenticationToken authenticationFor(long userId) {
    return new JwtAuthenticationToken(
        "access-token",
        new UserPrincipal(userId),
        new JwtPayload(
            "jti-1",
            String.valueOf(userId),
            Instant.parse("2026-05-26T00:00:00Z"),
            Instant.parse("2026-05-26T01:00:00Z"),
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
