package com.somagochi.pochakfarm.user.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.somagochi.pochakfarm.user.application.UserService;
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

  @MockitoBean private UserService userService;

  @Test
  void returnsCurrentUserWhenAuthenticated() throws Exception {
    given(userService.getProfile(1L))
        .willReturn(new UserResponse(1L, "user@example.com", SocialProvider.KAKAO));

    mockMvc
        .perform(get("/api/users/me").with(authentication(authenticationFor(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value("user@example.com"))
        .andExpect(jsonPath("$.data.provider").value("KAKAO"));
  }

  @Test
  void mapsBusinessExceptionToErrorResponse() throws Exception {
    given(userService.getProfile(1L)).willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

    mockMvc
        .perform(get("/api/users/me").with(authentication(authenticationFor(1L))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
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
