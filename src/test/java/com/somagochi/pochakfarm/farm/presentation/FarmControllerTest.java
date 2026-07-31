package com.somagochi.pochakfarm.farm.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.config.SecurityConfig;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.exception.GlobalExceptionHandler;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationToken;
import com.somagochi.pochakfarm.common.security.SecurityAccessDeniedHandler;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.farm.application.FarmQueryService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.dto.FarmAnimalResponse;
import com.somagochi.pochakfarm.farm.dto.FarmFloorResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSlotResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSpaceResponse;
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

@WebMvcTest(FarmController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  FarmControllerTest.TestConfig.class
})
class FarmControllerTest {

  private static final Long USER_ID = 1L;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FarmQueryService farmQueryService;

  @Test
  void returnsFarmSpaceOfTheme() throws Exception {
    given(farmQueryService.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE))
        .willReturn(response());

    mockMvc
        .perform(get("/api/farms/{theme}", "SEA").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.type").value("SEA"))
        .andExpect(jsonPath("$.data.page").value(FarmSpace.FIRST_PAGE))
        .andExpect(jsonPath("$.data.size").value(FarmSpace.FLOOR_COUNT_PER_PAGE))
        .andExpect(jsonPath("$.data.totalPages").value(FarmSpace.TOTAL_PAGE_COUNT))
        .andExpect(jsonPath("$.data.floors.length()").value(2))
        .andExpect(jsonPath("$.data.floors[0].unlocked").value(true))
        .andExpect(jsonPath("$.data.floors[0].slots[0].slotNum").value(1))
        .andExpect(jsonPath("$.data.floors[0].slots[0].animal.animalId").value(11))
        .andExpect(jsonPath("$.data.floors[0].slots[1].slotNum").value(2))
        .andExpect(jsonPath("$.data.floors[0].slots[1].animal").doesNotExist())
        .andExpect(jsonPath("$.data.floors[1].unlocked").value(false))
        .andExpect(jsonPath("$.data.floors[1].slots.length()").value(0));
  }

  @Test
  void acceptsLowerCaseTheme() throws Exception {
    given(farmQueryService.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE))
        .willReturn(response());

    mockMvc
        .perform(get("/api/farms/{theme}", "sea").with(authentication(userAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.type").value("SEA"));
  }

  @Test
  void returnsBadRequestForUnknownTheme() throws Exception {
    mockMvc
        .perform(get("/api/farms/{theme}", "river").with(authentication(userAuthentication())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
  }

  @Test
  void returnsNotFoundWhenSpaceMissing() throws Exception {
    given(farmQueryService.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE))
        .willThrow(new BusinessException(ErrorCode.FARM_SPACE_NOT_FOUND));

    mockMvc
        .perform(get("/api/farms/{theme}", "SEA").with(authentication(userAuthentication())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("FARM_SPACE_NOT_FOUND"));
  }

  @Test
  void returnsBadRequestWhenPageIsOutOfRange() throws Exception {
    given(farmQueryService.getFarmSpace(USER_ID, CardType.SEA, 1))
        .willThrow(new BusinessException(ErrorCode.INVALID_PARAMETER));

    mockMvc
        .perform(
            get("/api/farms/{type}", "SEA")
                .param("page", "1")
                .with(authentication(userAuthentication())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
  }

  @Test
  void returnsUnauthorizedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/farms/{theme}", "SEA")).andExpect(status().isUnauthorized());
  }

  private static Authentication userAuthentication() {
    return new JwtAuthenticationToken("token", new UserPrincipal(USER_ID), null);
  }

  private static FarmSpaceResponse response() {
    FarmFloorResponse firstFloor =
        new FarmFloorResponse(
            1,
            true,
            List.of(
                new FarmSlotResponse(
                    1,
                    new FarmAnimalResponse(
                        11L,
                        "첫번째",
                        "https://cdn.example.com/a.png",
                        "https://cdn.example.com/a-cutout.png")),
                FarmSlotResponse.empty(2)));
    return new FarmSpaceResponse(
        CardType.SEA,
        FarmSpace.FIRST_PAGE,
        FarmSpace.FLOOR_COUNT_PER_PAGE,
        FarmSpace.TOTAL_PAGE_COUNT,
        List.of(firstFloor, new FarmFloorResponse(2, false, List.of())));
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
