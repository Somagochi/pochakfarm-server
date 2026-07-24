package com.somagochi.pochakfarm.animal.presentation;

import com.somagochi.pochakfarm.animal.application.AnimalSlotMoveService;
import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveRequest;
import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor
public class AnimalController implements AnimalApiSpec {

  private final AnimalSlotMoveService animalSlotMoveService;

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
