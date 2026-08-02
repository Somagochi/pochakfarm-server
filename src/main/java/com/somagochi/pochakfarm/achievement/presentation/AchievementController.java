package com.somagochi.pochakfarm.achievement.presentation;

import com.somagochi.pochakfarm.achievement.application.AchievementClaimService;
import com.somagochi.pochakfarm.achievement.application.AchievementQueryService;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.dto.AchievementClaimResponse;
import com.somagochi.pochakfarm.achievement.dto.AchievementResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController implements AchievementApiSpec {

  private final AchievementQueryService achievementQueryService;
  private final AchievementClaimService achievementClaimService;

  @Override
  @GetMapping
  public ApiResponse<CursorPage<AchievementResponse>> getAchievements(
      @RequestParam(name = "category", required = false) AchievementCategory category,
      @RequestParam(name = "cursor", required = false) Long cursor,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(
        achievementQueryService.getAchievements(principal.id(), category, cursor));
  }

  @Override
  @PostMapping("/{code}/claim")
  public ApiResponse<AchievementClaimResponse> claimReward(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable String code) {
    return ApiResponse.success(achievementClaimService.claim(principal.id(), code));
  }
}
