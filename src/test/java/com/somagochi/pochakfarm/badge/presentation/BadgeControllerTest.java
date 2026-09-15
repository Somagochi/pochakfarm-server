package com.somagochi.pochakfarm.badge.presentation;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.badge.application.BadgeQueryService;
import com.somagochi.pochakfarm.badge.dto.OwnedBadgeResponse;
import com.somagochi.pochakfarm.common.config.SecurityConfig;
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
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BadgeController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  BadgeControllerTest.TestConfig.class
})
class BadgeControllerTest {
  private static final Long USER_ID = 1L;
  @Autowired private MockMvc mockMvc;
  @MockitoBean private BadgeQueryService service;

  @Test
  void returnsOwnedBadgesForAuthenticatedUser() throws Exception {
    given(service.getOwnedBadges(USER_ID))
        .willReturn(
            List.of(
                new OwnedBadgeResponse(
                    "BDG001",
                    "첫 걸음",
                    "첫 업적 보상",
                    "https://cdn.test/badge.png",
                    Instant.parse("2026-09-15T00:00:00Z"))));
    mockMvc
        .perform(
            get("/api/badges").param("userId", "999").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].code").value("BDG001"))
        .andExpect(jsonPath("$.data[0].name").value("첫 걸음"))
        .andExpect(jsonPath("$.data[0].description").value("첫 업적 보상"))
        .andExpect(jsonPath("$.data[0].imageUrl").value("https://cdn.test/badge.png"))
        .andExpect(jsonPath("$.data[0].acquiredAt").value("2026-09-15T00:00:00Z"));
    verify(service).getOwnedBadges(USER_ID);
  }

  @Test
  void returnsEmptyArray() throws Exception {
    given(service.getOwnedBadges(USER_ID)).willReturn(List.of());
    mockMvc
        .perform(get("/api/badges").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void includesExplicitNullImageUrl() throws Exception {
    given(service.getOwnedBadges(USER_ID))
        .willReturn(List.of(new OwnedBadgeResponse("BDG001", "첫 걸음", null, null, Instant.EPOCH)));
    mockMvc
        .perform(get("/api/badges").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].imageUrl").hasJsonPath())
        .andExpect(jsonPath("$.data[0].imageUrl").value(nullValue()));
  }

  @Test
  void rejectsUnauthenticatedRequest() throws Exception {
    mockMvc.perform(get("/api/badges")).andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
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
