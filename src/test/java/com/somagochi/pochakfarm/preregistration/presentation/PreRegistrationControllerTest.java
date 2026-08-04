package com.somagochi.pochakfarm.preregistration.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.common.config.SecurityConfig;
import com.somagochi.pochakfarm.common.exception.GlobalExceptionHandler;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.SecurityAccessDeniedHandler;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationCancelService;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationCouponSmsService;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationRegisterService;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationCouponSmsResult;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = PreRegistrationController.class,
    properties = "app.admin.api-key=test-admin-key")
@Import({
  SecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  SecurityAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  com.somagochi.pochakfarm.common.config.AdminApiConfig.class,
  com.somagochi.pochakfarm.common.security.AdminApiKeyValidator.class,
  PreRegistrationControllerTest.TestConfig.class
})
class PreRegistrationControllerTest {

  private static final String TEMPLATE = "[포착팜] 쿠폰 코드: {couponCode}";
  private static final String BODY = "{\"messageTemplate\":\"[포착팜] 쿠폰 코드: {couponCode}\"}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PreRegistrationRegisterService preRegistrationRegisterService;

  @MockitoBean private PreRegistrationCancelService preRegistrationCancelService;

  @MockitoBean private PreRegistrationCouponSmsService preRegistrationCouponSmsService;

  @Test
  void couponSmsDefaultsToDryRun() throws Exception {
    given(preRegistrationCouponSmsService.send(true, TEMPLATE))
        .willReturn(new PreRegistrationCouponSmsResult(10, 0, 0, 0, true));

    mockMvc
        .perform(
            post("/api/pre-registrations/coupon-sms")
                .header("X-Admin-Key", "test-admin-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(authentication(authenticationOf())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.dryRun").value(true))
        .andExpect(jsonPath("$.data.targetCount").value(10));
  }

  @Test
  void couponSmsSendsWhenDryRunDisabled() throws Exception {
    given(preRegistrationCouponSmsService.send(false, TEMPLATE))
        .willReturn(new PreRegistrationCouponSmsResult(10, 9, 1, 0, false));

    mockMvc
        .perform(
            post("/api/pre-registrations/coupon-sms?dryRun=false")
                .header("X-Admin-Key", "test-admin-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(authentication(authenticationOf())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sentCount").value(9))
        .andExpect(jsonPath("$.data.failedCount").value(1));
  }

  @Test
  void couponSmsRejectsInvalidAdminKey() throws Exception {
    mockMvc
        .perform(
            post("/api/pre-registrations/coupon-sms")
                .header("X-Admin-Key", "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(authentication(authenticationOf())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN_ADMIN_ACCESS"));

    verify(preRegistrationCouponSmsService, never()).send(anyBoolean(), any());
  }

  @Test
  void couponSmsRejectsMissingAdminKey() throws Exception {
    mockMvc
        .perform(
            post("/api/pre-registrations/coupon-sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(authentication(authenticationOf())))
        .andExpect(status().isForbidden());

    verify(preRegistrationCouponSmsService, never()).send(anyBoolean(), any());
  }

  @Test
  void couponSmsRejectsWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/pre-registrations/coupon-sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isUnauthorized());

    verify(preRegistrationCouponSmsService, never()).send(anyBoolean(), any());
  }

  private Authentication authenticationOf() {
    return new UsernamePasswordAuthenticationToken(new UserPrincipal(7L), null, List.of());
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
