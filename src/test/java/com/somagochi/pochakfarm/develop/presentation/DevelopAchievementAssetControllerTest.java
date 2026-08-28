package com.somagochi.pochakfarm.develop.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.somagochi.pochakfarm.achievement.application.AchievementReconciliationService;
import com.somagochi.pochakfarm.achievement.dto.AchievementReconciliationResult;
import com.somagochi.pochakfarm.develop.application.DevelopAchievementAssetService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DevelopAchievementAssetControllerTest {

  private final DevelopAchievementAssetService assetService =
      mock(DevelopAchievementAssetService.class);
  private final AchievementReconciliationService reconciliationService =
      mock(AchievementReconciliationService.class);
  private final MockMvc mockMvc =
      MockMvcBuilders.standaloneSetup(
              new DevelopAchievementAssetController(assetService, reconciliationService))
          .build();

  @Test
  void reconcilesSingleAndMultipleUsersThroughOneEndpoint() throws Exception {
    given(reconciliationService.reconcile(List.of(1L, 2L, 1L)))
        .willReturn(new AchievementReconciliationResult(3, 2, 1, List.of(2L)));

    mockMvc
        .perform(
            post("/api/dev/achievements/reconciliation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userIds\":[1,2,1]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.requestedCount").value(3))
        .andExpect(jsonPath("$.data.distinctCount").value(2))
        .andExpect(jsonPath("$.data.succeededCount").value(1))
        .andExpect(jsonPath("$.data.failedUserIds[0]").value(2));

    verify(reconciliationService).reconcile(List.of(1L, 2L, 1L));
  }
}
