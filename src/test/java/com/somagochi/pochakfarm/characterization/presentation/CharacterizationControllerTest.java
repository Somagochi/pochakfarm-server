package com.somagochi.pochakfarm.characterization.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.characterization.application.CharacterizationReadService;
import com.somagochi.pochakfarm.characterization.application.CharacterizationService;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationStartResponse;
import com.somagochi.pochakfarm.common.config.SecurityConfig;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.exception.GlobalExceptionHandler;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.SecurityAccessDeniedHandler;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import com.somagochi.pochakfarm.device.application.DeviceService;
import com.somagochi.pochakfarm.device.application.DeviceService.DeviceResolution;
import com.somagochi.pochakfarm.device.domain.AnonymousDevice;
import com.somagochi.pochakfarm.device.presentation.DeviceTokenCookieFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(CharacterizationController.class)
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  DeviceTokenCookieFactory.class,
  CharacterizationControllerTest.TestConfig.class
})
class CharacterizationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CharacterizationService characterizationService;

  @MockitoBean private CharacterizationReadService characterizationReadService;

  @MockitoBean private DeviceService deviceService;

  @Test
  void issuesDeviceTokenCookieWhenMissing() throws Exception {
    AnonymousDevice device = device(1L, "dev_new");
    given(deviceService.resolveOrIssue(null)).willReturn(new DeviceResolution(device, true));
    given(characterizationService.characterize(eq(1L), any(MultipartFile.class), eq("솜구름")))
        .willReturn(response());

    mockMvc
        .perform(
            multipart("/api/characterizations/public")
                .file(image())
                .param("animalName", "솜구름")
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    HttpHeaders.SET_COOKIE,
                    org.hamcrest.Matchers.containsString("deviceToken=dev_new")))
        .andExpect(jsonPath("$.data.aiImageUrl").doesNotExist())
        .andExpect(jsonPath("$.data.characterizationId").value(100L))
        .andExpect(jsonPath("$.data.status").value("PROCESSING"))
        .andExpect(jsonPath("$.data.cardType").value("SKY"))
        .andExpect(jsonPath("$.data.resultImageUrl").doesNotExist())
        .andExpect(jsonPath("$.data.cardBackImageUrl").doesNotExist());
  }

  @Test
  void usesExistingDeviceTokenCookie() throws Exception {
    AnonymousDevice device = device(1L, "dev_abc");
    given(deviceService.resolveOrIssue("dev_abc")).willReturn(new DeviceResolution(device, false));
    given(characterizationService.characterize(eq(1L), any(MultipartFile.class), eq("솜구름")))
        .willReturn(response());

    mockMvc
        .perform(
            multipart("/api/characterizations/public")
                .file(image())
                .param("animalName", "솜구름")
                .cookie(new Cookie(DeviceTokenCookieFactory.COOKIE_NAME, "dev_abc"))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
        .andExpect(jsonPath("$.data.characterizationId").value(100L))
        .andExpect(jsonPath("$.data.status").value("PROCESSING"))
        .andExpect(jsonPath("$.data.cardType").value("SKY"))
        .andExpect(jsonPath("$.data.cardBackImageUrl").doesNotExist());
  }

  @Test
  void returnsCharacterizationById() throws Exception {
    given(characterizationReadService.getCharacterization(100L)).willReturn(resultResponse());

    mockMvc
        .perform(get("/api/characterizations/public/{characterizationId}", 100L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.characterizationId").value(100L))
        .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
        .andExpect(jsonPath("$.data.resultImageUrl").value("https://cdn.test/result.png"))
        .andExpect(jsonPath("$.data.cardType").doesNotExist())
        .andExpect(jsonPath("$.data.cardBackImageUrl").doesNotExist());
  }

  @Test
  void returnsNotFoundWhenCharacterizationMissing() throws Exception {
    given(characterizationReadService.getCharacterization(999L))
        .willThrow(new BusinessException(ErrorCode.CHARACTERIZATION_NOT_FOUND));

    mockMvc
        .perform(get("/api/characterizations/public/{characterizationId}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CHARACTERIZATION_NOT_FOUND"));
  }

  private static MockMultipartFile image() {
    return new MockMultipartFile("image", "animal.png", "image/png", "fake-image".getBytes());
  }

  private static CharacterizationStartResponse response() {
    return new CharacterizationStartResponse(100L, CharacterizationStatus.PROCESSING, CardType.SKY);
  }

  private static CharacterizationResponse resultResponse() {
    return new CharacterizationResponse(
        100L, CharacterizationStatus.SUCCEEDED, "https://cdn.test/result.png", null);
  }

  private static AnonymousDevice device(Long id, String deviceToken) {
    AnonymousDevice device = org.mockito.Mockito.mock(AnonymousDevice.class);
    given(device.getId()).willReturn(id);
    given(device.getDeviceToken()).willReturn(deviceToken);
    return device;
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
