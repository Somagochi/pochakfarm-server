package com.somagochi.pochakfarm.develop.presentation;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.develop.application.DevelopDeviceService;
import com.somagochi.pochakfarm.develop.application.DevelopLoginService;
import com.somagochi.pochakfarm.develop.application.DevelopSampleAnimalService;
import com.somagochi.pochakfarm.develop.config.DevelopSecurityConfig;
import com.somagochi.pochakfarm.develop.dto.DevelopDeviceTokenResponse;
import com.somagochi.pochakfarm.develop.dto.DevelopSampleAnimalResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevelopController.class)
@Import({
  DevelopSecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  DevelopControllerTest.TestConfig.class
})
@ActiveProfiles("local")
class DevelopControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DevelopLoginService developLoginService;

  @MockitoBean private DevelopDeviceService developDeviceService;

  @MockitoBean private DevelopSampleAnimalService developSampleAnimalService;

  @Test
  void issuesTokenPairWithoutAuthentication() throws Exception {
    given(developLoginService.login(5L))
        .willReturn(
            new SocialLoginResponse(
                new TokenResponse("access-token", "refresh-token"), false, false));

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

  @Test
  void createsSampleAnimalForAuthenticatedUser() throws Exception {
    given(developSampleAnimalService.createSampleAnimal(7L))
        .willReturn(new DevelopSampleAnimalResponse(9));

    mockMvc
        .perform(post("/api/dev/animal").with(authentication(authenticationOf(7L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.createdCount").value(9));
  }

  @Test
  void rejectsSampleAnimalWithoutAuthentication() throws Exception {
    mockMvc.perform(post("/api/dev/animal")).andExpect(status().isUnauthorized());

    verify(developSampleAnimalService, never()).createSampleAnimal(anyLong());
  }

  private Authentication authenticationOf(Long userId) {
    UserPrincipal principal = new UserPrincipal(userId);
    return new UsernamePasswordAuthenticationToken(principal, null, List.of());
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
