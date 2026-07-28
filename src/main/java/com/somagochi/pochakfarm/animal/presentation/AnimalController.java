package com.somagochi.pochakfarm.animal.presentation;

import com.somagochi.pochakfarm.animal.application.AnimalDeleteService;
import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.application.AnimalSlotMoveService;
import com.somagochi.pochakfarm.animal.dto.AnimalDetailResponse;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveRequest;
import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor
public class AnimalController implements AnimalApiSpec {

  private final AnimalQueryService animalQueryService;
  private final AnimalDeleteService animalDeleteService;
  private final AnimalSlotMoveService animalSlotMoveService;

  @Override
  @GetMapping("/{animalId}")
  public ApiResponse<AnimalDetailResponse> getAnimal(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long animalId) {
    return ApiResponse.success(animalQueryService.getAnimal(principal.id(), animalId));
  }

  @Override
  @DeleteMapping("/{animalId}")
  public ApiResponse<Void> deleteAnimal(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long animalId) {
    animalDeleteService.deleteAnimal(principal.id(), animalId);
    return ApiResponse.empty();
  }

  @Override
  @GetMapping
  public ApiResponse<CursorPage<AnimalResponse>> getMyAnimals(
      @RequestParam(name = "type", required = false) CardType type,
      @RequestParam(name = "cursor", required = false) Long cursor,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(animalQueryService.getMyAnimals(principal.id(), type, cursor));
  }

  @Override
  @PatchMapping("/{animalId}/slot")
  public ApiResponse<AnimalSlotMoveResponse> moveSlot(
          @PathVariable Long animalId,
          @RequestBody AnimalSlotMoveRequest request,
          @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(
            animalSlotMoveService.moveToSlot(principal.id(), animalId, request.targetSlotId()));
  }
}
