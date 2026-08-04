package com.somagochi.pochakfarm.develop.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.common.notification.Notification;
import com.somagochi.pochakfarm.common.notification.NotificationService;
import com.somagochi.pochakfarm.common.notification.SmsNotification;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import com.somagochi.pochakfarm.develop.config.DevelopSecurityConfig;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevelopNotificationController.class)
@Import({
  DevelopSecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  DevelopNotificationControllerTest.TestConfig.class
})
@ActiveProfiles("local")
class DevelopNotificationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private NotificationService notificationService;

  @Test
  void sendsSmsWithBasicAuth() throws Exception {
    mockMvc
        .perform(
            post("/api/dev/notifications/sms")
                .with(httpBasic("dev-admin", "dev-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"01012345678\",\"text\":\"테스트 메시지\"}"))
        .andExpect(status().isOk());

    verify(notificationService).notify(new SmsNotification("01012345678", "테스트 메시지"));
  }

  @Test
  void rejectsWithoutBasicAuth() throws Exception {
    mockMvc
        .perform(
            post("/api/dev/notifications/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"01012345678\",\"text\":\"테스트 메시지\"}"))
        .andExpect(status().isUnauthorized());

    verify(notificationService, never()).notify(any(Notification.class));
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

    @Bean
    PasswordEncoder passwordEncoder() {
      return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
      return new InMemoryUserDetailsManager(
          User.withUsername("dev-admin")
              .password(passwordEncoder.encode("dev-password"))
              .roles("DEV_ADMIN")
              .build());
    }
  }
}
