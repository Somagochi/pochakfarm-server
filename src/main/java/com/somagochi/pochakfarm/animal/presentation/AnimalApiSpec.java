package com.somagochi.pochakfarm.animal.presentation;

import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.response.CursorPage;
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
      summary = "내 동물 조회",
      description =
          "로그인한 사용자의 농장에 배치된 동물을 커서 기반으로 조회한다(최신순, 한 페이지 12개). "
              + "type 을 지정하면 해당 카드 타입만, cursor 에 이전 응답의 nextCursor 를 넘기면 다음 페이지를 반환한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "지원하지 않는 타입 값",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CursorPage<AnimalResponse>> getMyAnimals(
      @Parameter(description = "카드 타입 필터. 생략하면 배치된 전체 동물", example = "SEA") CardType type,
      @Parameter(description = "다음 페이지 커서(이전 응답의 nextCursor). 첫 페이지는 생략", example = "38")
          Long cursor,
      UserPrincipal principal);
}
