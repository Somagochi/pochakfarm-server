package com.somagochi.pochakfarm.develop.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import com.somagochi.pochakfarm.develop.application.DevelopAchievementAssetService;
import com.somagochi.pochakfarm.develop.application.DevelopGymLeaderAssetService;
import com.somagochi.pochakfarm.develop.config.DevelopSecurityConfig;
import com.somagochi.pochakfarm.develop.dto.DevelopAchievementAssetView;
import com.somagochi.pochakfarm.develop.dto.DevelopAchievementRewardView;
import com.somagochi.pochakfarm.develop.dto.DevelopGymLeaderAnimalView;
import com.somagochi.pochakfarm.develop.dto.DevelopGymLeaderAssetView;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

@WebMvcTest(DevelopAssetController.class)
@Import({
  DevelopSecurityConfig.class,
  SecurityAuthenticationEntryPoint.class,
  DevelopAssetControllerTest.TestConfig.class
})
@ActiveProfiles("local")
class DevelopAssetControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DevelopAchievementAssetService developAchievementAssetService;
  @MockitoBean private DevelopGymLeaderAssetService developGymLeaderAssetService;
  @MockitoBean private ImageUploadService imageUploadService;

  @BeforeEach
  void setUp() {
    when(developAchievementAssetService.getAssets()).thenReturn(List.of());
    when(developGymLeaderAssetService.getAssets()).thenReturn(List.of());
  }

  @Test
  void rendersAchievementTabByDefault() throws Exception {
    mockMvc
        .perform(get("/api/dev/assets").with(httpBasic("dev-admin", "dev-password")))
        .andExpect(status().isOk())
        .andExpect(view().name("develop/assets"))
        .andExpect(model().attribute("tab", "achievement"))
        .andExpect(model().attributeExists("achievements", "gymLeaders"));
  }

  @Test
  void rendersGymLeaderTabWhenRequested() throws Exception {
    mockMvc
        .perform(
            get("/api/dev/assets")
                .param("tab", "gym-leader")
                .with(httpBasic("dev-admin", "dev-password")))
        .andExpect(status().isOk())
        .andExpect(model().attribute("tab", "gym-leader"));
  }

  @Test
  void presignsWithTargetPurpose() throws Exception {
    when(imageUploadService.createPublicPresign("gym-leader-animal", "image/png"))
        .thenReturn(
            new PresignResponse(
                "https://s3/upload", "public/gym-leader-animal/a.png", Instant.EPOCH));

    mockMvc
        .perform(
            post("/api/dev/assets/presign")
                .with(httpBasic("dev-admin", "dev-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\":\"image/png\",\"target\":\"GYM_LEADER_ANIMAL\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.key").value("public/gym-leader-animal/a.png"));
  }

  @Test
  void updatesGymLeaderImagesAndRedirectsToGymLeaderTab() throws Exception {
    mockMvc
        .perform(
            post("/api/dev/assets/gym-leaders/1/images")
                .with(httpBasic("dev-admin", "dev-password"))
                .param("thumbnailKey", "public/gym-leader-thumbnail/a.png")
                .param("thumbnailContentType", "image/png")
                .param("imageKey", "public/gym-leader/a.png")
                .param("imageContentType", "image/png"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/api/dev/assets?tab=gym-leader"));

    verify(developGymLeaderAssetService)
        .updateGymLeaderImages(
            1L,
            "public/gym-leader-thumbnail/a.png",
            "image/png",
            "public/gym-leader/a.png",
            "image/png");
  }

  @Test
  void updatesOnlySelectedGymLeaderImage() throws Exception {
    mockMvc
        .perform(
            post("/api/dev/assets/gym-leaders/1/images")
                .with(httpBasic("dev-admin", "dev-password"))
                .param("thumbnailKey", "public/gym-leader-thumbnail/a.png")
                .param("thumbnailContentType", "image/png"))
        .andExpect(status().is3xxRedirection());

    verify(developGymLeaderAssetService)
        .updateGymLeaderImages(1L, "public/gym-leader-thumbnail/a.png", "image/png", null, null);
  }

  @Test
  void updatesRewardAmountAndRedirectsToAchievementTab() throws Exception {
    mockMvc
        .perform(
            post("/api/dev/assets/achievements/rewards/2/amount")
                .with(httpBasic("dev-admin", "dev-password"))
                .param("amount", "100"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/api/dev/assets?tab=achievement"));

    verify(developAchievementAssetService).updateRewardAmount(2L, 100L);
  }

  @Test
  void rendersBothTabContents() throws Exception {
    when(developAchievementAssetService.getAssets())
        .thenReturn(
            List.of(
                new DevelopAchievementAssetView(
                    1L,
                    "ACH001",
                    "첫 포착",
                    "동물을 처음 포착한다",
                    AchievementCategory.COLLECTION,
                    false,
                    1L,
                    null,
                    "https://cdn/unachieved.png",
                    null,
                    "https://cdn/achieved.png",
                    List.of(
                        new DevelopAchievementRewardView(
                            10L, RewardType.COIN, 100L, null, null, null),
                        new DevelopAchievementRewardView(
                            11L,
                            RewardType.BADGE,
                            null,
                            "BDG001",
                            "첫 포착 뱃지",
                            "https://cdn/badge.png")))));
    when(developGymLeaderAssetService.getAssets())
        .thenReturn(
            List.of(
                new DevelopGymLeaderAssetView(
                    2L,
                    "GYM_01",
                    "새싹 관장 두더",
                    1,
                    null,
                    "https://cdn/gym-thumbnail.png",
                    null,
                    "https://cdn/gym.png",
                    "BDG006",
                    "새싹 뱃지",
                    null,
                    List.of(
                        new DevelopGymLeaderAnimalView(
                            20L,
                            1,
                            "도톨",
                            CardType.GROUND,
                            CardType.GROUND.label(),
                            Tier.C,
                            "나뭇잎 방어",
                            SkillBattleType.STABLE,
                            "이끼 쿠션",
                            SkillBattleType.BALANCED,
                            null,
                            null)))));

    mockMvc
        .perform(get("/api/dev/assets").with(httpBasic("dev-admin", "dev-password")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ACH001")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("BDG001 · 첫 포착 뱃지")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("GYM_01")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("https://cdn/gym-thumbnail.png")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("BDG006 · 새싹 뱃지")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("도톨")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"skill stable\"")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("class=\"skill balanced\"")));
  }

  @Test
  void rejectsWithoutBasicAuth() throws Exception {
    mockMvc.perform(get("/api/dev/assets")).andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/dev/assets/gym-leaders/1/images")
                .param("imageKey", "public/gym-leader/a.png")
                .param("imageContentType", "image/png"))
        .andExpect(status().isUnauthorized());

    verify(developGymLeaderAssetService, never())
        .updateGymLeaderImages(anyLong(), any(), any(), anyString(), anyString());
    verify(imageUploadService, never()).createPublicPresign(any(), any());
    verify(imageUploadService, never()).createPublicPresign(eq("achievement"), any());
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
