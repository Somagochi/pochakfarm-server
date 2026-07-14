package com.somagochi.pochakfarm.share.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.characterization.application.CharacterizationReadService;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.common.config.SecurityConfig;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.exception.GlobalExceptionHandler;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.SecurityAccessDeniedHandler;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShareController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  ShareControllerTest.TestConfig.class
})
class ShareControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CharacterizationReadService characterizationReadService;

  @Test
  void rendersOpenGraphHtml() throws Exception {
    BDDMockito.given(characterizationReadService.getCharacterization(100L))
        .willReturn(
            new CharacterizationResponse(
                100L, CharacterizationStatus.SUCCEEDED, "https://cdn.test/result.png", null));

    mockMvc
        .perform(
            get("/share/characterizations/{characterizationId}", 100L)
                .header("X-Forwarded-Proto", "https")
                .header("Host", "share.pochakfarm.com"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"))
        .andExpect(
            content()
                .string(Matchers.containsString("<meta property=\"og:type\" content=\"website\"")))
        .andExpect(
            content()
                .string(Matchers.containsString("<meta property=\"og:title\" content=\"포착팜\"")))
        .andExpect(
            content().string(Matchers.containsString("content=\"https://cdn.test/result.png\"")))
        .andExpect(
            content()
                .string(
                    Matchers.containsString(
                        "content=\"https://share.pochakfarm.com/share/characterizations/100\"")))
        .andExpect(
            content()
                .string(
                    Matchers.containsString(
                        "https://pochakfarm-promotion.vercel.app/result?characterization_id=")));
  }

  @Test
  void returnsNotFoundWhenCharacterizationMissing() throws Exception {
    BDDMockito.given(characterizationReadService.getCharacterization(999L))
        .willThrow(new BusinessException(ErrorCode.CHARACTERIZATION_NOT_FOUND));

    mockMvc
        .perform(get("/share/characterizations/{characterizationId}", 999L))
        .andExpect(status().isNotFound());
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
