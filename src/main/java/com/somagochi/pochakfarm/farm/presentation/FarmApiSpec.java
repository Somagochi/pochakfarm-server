package com.somagochi.pochakfarm.farm.presentation;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.farm.dto.FarmFloorPurchaseResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSpaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Farm", description = "농장 관련 API")
public interface FarmApiSpec {

  @Operation(
      summary = "테마별 농장 페이지 조회",
      description =
          "로그인한 사용자의 해당 테마 농장을 조회한다. 한 페이지는 4개 층, 한 층은 4개 슬롯으로 고정이며 순번 오름차순이다. "
              + "개방하지 않은 층은 unlocked=false 와 빈 슬롯 목록으로 응답한다. "
              + "개방한 층의 슬롯은 순번 오름차순이며, 동물이 배치되지 않은 슬롯의 animal 은 null 이다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "지원하지 않는 테마 값 또는 범위를 벗어난 페이지",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "해당 테마의 농장 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<FarmSpaceResponse> getFarmSpace(
      @Parameter(description = "농장 테마", example = "SEA") CardType type,
      @Parameter(description = "페이지 번호(0부터 시작). 생략하면 0. 한 페이지 층 수는 4로 고정한다.", example = "0")
          int page,
      UserPrincipal principal);

  @Operation(
      summary = "농장 다음 층 구매",
      description =
          "로그인한 사용자의 해당 테마 농장에서 현재 개방된 층의 바로 다음 층을 코인으로 구매해 개방한다. "
              + "층당 가격은 1,000코인으로 동일하며 순차 개방만 가능하다. "
              + "성공 시 새로 개방된 층 번호와 남은 코인을 응답한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구매 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "지원하지 않는 테마 값",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "해당 테마의 농장 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description = "모든 층 개방 완료, 코인 부족 또는 동시 구매 충돌",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<FarmFloorPurchaseResponse> purchaseNextFloor(
      @Parameter(description = "농장 테마", example = "SEA") CardType type, UserPrincipal principal);
}
