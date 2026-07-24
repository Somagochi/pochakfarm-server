package com.somagochi.pochakfarm.animal.presentation;

import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveRequest;
import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveResponse;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Animal", description = "동물 관련 API")
public interface AnimalApiSpec {

  @Operation(
      summary = "동물 슬롯 위치 교체",
      description =
          "로그인한 사용자의 동물을 대상 슬롯으로 이동한다. 대상 슬롯이 비어 있으면 그대로 이동하고, "
              + "사용자의 다른 동물이 있으면 두 동물의 슬롯을 서로 교체한다. 동물의 카드 타입과 대상 슬롯 농장의 테마가 같아야 한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이동 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "동물 카드 타입과 대상 슬롯 테마 불일치",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "다른 사용자의 동물 또는 슬롯 접근",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "동물 또는 대상 슬롯 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<AnimalSlotMoveResponse> moveSlot(
      @Parameter(description = "이동할 동물 ID", example = "1") Long animalId,
      AnimalSlotMoveRequest request,
      UserPrincipal principal);
}
