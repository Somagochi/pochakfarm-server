package com.somagochi.pochakfarm.animal.presentation;

import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.characterization.domain.CardType;
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
@RequestMapping("/api/animals")
@RequiredArgsConstructor
public class AnimalController implements AnimalApiSpec {

  private final AnimalQueryService animalQueryService;

  @Override
  @GetMapping
  public ApiResponse<CursorPage<AnimalResponse>> getMyAnimals(
      @RequestParam(name = "type", required = false) CardType type,
      @RequestParam(name = "cursor", required = false) Long cursor,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(animalQueryService.getMyAnimals(principal.id(), type, cursor));
  }
}
