package com.somagochi.pochakfarm.animal.presentation;

import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.dto.AnimalDetailResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor
public class AnimalController implements AnimalApiSpec {

  private final AnimalQueryService animalQueryService;

  @Override
  @GetMapping("/{animalId}")
  public ApiResponse<AnimalDetailResponse> getAnimal(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long animalId) {
    return ApiResponse.success(animalQueryService.getAnimal(principal.id(), animalId));
  }
}
