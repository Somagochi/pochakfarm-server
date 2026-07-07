package com.somagochi.pochakfarm.develop.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.develop.application.DevelopDeviceService;
import com.somagochi.pochakfarm.develop.application.DevelopLoginService;
import com.somagochi.pochakfarm.develop.config.DevelopSecurityConfig;
import com.somagochi.pochakfarm.develop.dto.DevelopDeviceTokenResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevelopController.class)
@Import({DevelopSecurityConfig.class, DevelopControllerTest.TestConfig.class})
@ActiveProfiles("local")
class DevelopControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DevelopLoginService developLoginService;

  @MockitoBean private DevelopDeviceService developDeviceService;

  @Test
  void issuesTokenPairWithoutAuthentication() throws Exception {
    given(developLoginService.login(5L))
        .willReturn(
            new SocialLoginResponse(new TokenResponse("access-token", "refresh-token"), false));

    mockMvc
        .perform(post("/api/dev/login/5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token.accessToken").value("access-token"))
        .andExpect(jsonPath("$.data.token.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.data.isNew").value(false));
  }

  @Test
  void issuesDeviceTokenWithoutAuthentication() throws Exception {
    given(developDeviceService.issueDeviceToken())
        .willReturn(new DevelopDeviceTokenResponse("dev_abc"));

    mockMvc
        .perform(post("/api/dev/device-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.deviceToken").value("dev_abc"));
  }

  @TestConfiguration
  @EnableWebSecurity
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
