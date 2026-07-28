package com.somagochi.pochakfarm.animal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.animal.application.AnimalDeleteService;
import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.application.AnimalSlotMoveService;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.config.SecurityConfig;
import com.somagochi.pochakfarm.common.exception.GlobalExceptionHandler;
import com.somagochi.pochakfarm.common.response.CursorPage;
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

@WebMvcTest(AnimalController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  AnimalControllerTest.TestConfig.class
})
class AnimalControllerTest {

  private static final Long USER_ID = 1L;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AnimalQueryService animalQueryService;

  @MockitoBean private AnimalDeleteService animalDeleteService;

  @MockitoBean private AnimalSlotMoveService animalSlotMoveService;

  @Test
  void returnsMyAnimals() throws Exception {
    given(animalQueryService.getMyAnimals(USER_ID, null, null))
        .willReturn(CursorPage.of(List.of(animalResponse(11L, CardType.SEA)), 11L, true));

    mockMvc
        .perform(get("/api/animals").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].animalId").value(11))
        .andExpect(jsonPath("$.data.content[0].cardType").value("SEA"))
        .andExpect(jsonPath("$.data.nextCursor").value(11))
        .andExpect(jsonPath("$.data.hasNext").value(true));
  }

  @Test
  void filtersMyAnimalsByTypeAndCursor() throws Exception {
    given(animalQueryService.getMyAnimals(USER_ID, CardType.SEA, 50L))
        .willReturn(CursorPage.of(List.of(animalResponse(11L, CardType.SEA)), null, false));

    mockMvc
        .perform(
            get("/api/animals")
                .param("type", "SEA")
                .param("cursor", "50")
                .with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].cardType").value("SEA"))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void returnsUnauthorizedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/animals")).andExpect(status().isUnauthorized());
  }

  private static Authentication userAuthentication() {
    return new JwtAuthenticationToken("token", new UserPrincipal(USER_ID), null);
  }

  private static AnimalResponse animalResponse(Long animalId, CardType type) {
    return new AnimalResponse(
        animalId,
        "동물" + animalId,
        type,
        Tier.A,
        "https://cdn.example.com/card.png",
        "https://cdn.example.com/animal.png");
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
