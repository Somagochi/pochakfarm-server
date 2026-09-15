package com.somagochi.pochakfarm.badge.presentation;

import com.somagochi.pochakfarm.badge.application.BadgeQueryService;
import com.somagochi.pochakfarm.badge.dto.OwnedBadgeResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController implements BadgeApiSpec {
  private final BadgeQueryService badgeQueryService;

  @Override
  @GetMapping
  public ApiResponse<List<OwnedBadgeResponse>> getOwnedBadges(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(badgeQueryService.getOwnedBadges(principal.id()));
  }
}
