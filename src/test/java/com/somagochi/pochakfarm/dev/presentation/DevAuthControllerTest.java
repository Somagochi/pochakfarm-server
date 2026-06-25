package com.somagochi.pochakfarm.dev.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.dev.application.DevLoginService;
import com.somagochi.pochakfarm.dev.config.DevSecurityConfig;
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

@WebMvcTest(DevAuthController.class)
@Import({DevSecurityConfig.class, DevAuthControllerTest.TestConfig.class})
@ActiveProfiles("local")
class DevAuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DevLoginService devLoginService;

  @Test
  void issuesTokenPairWithoutAuthentication() throws Exception {
    given(devLoginService.login(5L))
        .willReturn(
            new SocialLoginResponse(new TokenResponse("access-token", "refresh-token"), false));

    mockMvc
        .perform(post("/api/dev/login/5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token.accessToken").value("access-token"))
        .andExpect(jsonPath("$.data.token.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.data.isNew").value(false));
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
