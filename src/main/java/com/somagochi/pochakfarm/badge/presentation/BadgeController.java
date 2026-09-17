package com.somagochi.pochakfarm.badge.presentation;

import com.somagochi.pochakfarm.badge.application.BadgeQueryService;
import com.somagochi.pochakfarm.badge.domain.BadgeCategory;
import com.somagochi.pochakfarm.badge.dto.OwnedBadgeResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController implements BadgeApiSpec {
  private final BadgeQueryService badgeQueryService;

  @Override
  @GetMapping
  public ApiResponse<CursorPage<OwnedBadgeResponse>> getOwnedBadges(
      @RequestParam(name = "category", required = false) BadgeCategory category,
      @RequestParam(name = "cursor", required = false) Long cursor,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(badgeQueryService.getOwnedBadges(principal.id(), category, cursor));
  }
}
