package com.somagochi.pochakfarm.farm.presentation;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.farm.application.FarmQueryService;
import com.somagochi.pochakfarm.farm.dto.FarmSpaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
public class FarmController implements FarmApiSpec {

  private final FarmQueryService farmQueryService;

  @Override
  @GetMapping("/{type}")
  public ApiResponse<FarmSpaceResponse> getFarmSpace(
      @PathVariable CardType type,
      @RequestParam(name = "page", required = false, defaultValue = "0") int page,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(farmQueryService.getFarmSpace(principal.id(), type, page));
  }
}
