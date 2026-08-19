package com.somagochi.pochakfarm.animal.presentation;

import com.somagochi.pochakfarm.animal.dto.AnimalDetailResponse;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveRequest;
import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveResponse;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

  @Operation(
      summary = "내 동물 검색",
      description =
          "로그인한 사용자가 보유한 동물을 이름으로 검색한다(최신순, 한 페이지 12개). "
              + "keyword 는 필수이며 keyword 로 시작하는 이름만 검색된다(전방 일치). "
              + "type 을 지정하면 해당 카드 타입만 검색하고, 생략하면 전체 타입을 검색한다. "
              + "cursor 에 이전 응답의 nextCursor 를 넘기면 다음 페이지를 반환한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "keyword 누락 또는 공백, 지원하지 않는 type 값 (INVALID_PARAMETER)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CursorPage<AnimalResponse>> searchMyAnimals(
      @Parameter(description = "카드 타입 필터. 생략하면 전체 타입", example = "SEA") CardType type,
      @Parameter(description = "동물 이름 전방 일치 검색어", required = true, example = "솜") String keyword,
      @Parameter(description = "다음 페이지 커서(이전 응답의 nextCursor). 첫 페이지는 생략", example = "38")
          Long cursor,
      UserPrincipal principal);

  @Operation(
      summary = "동물 상세 조회",
      description =
          "로그인한 사용자가 소유한 동물의 상세 정보를 조회한다. "
              + "animalId 로 해당 동물과 연결된 캡처 카드를 찾아 카드 타입, 티어, 스킬, 이미지 URL 을 반환한다. "
              + "스킬이 지정되지 않은 경우 skill1/skill2 는 null 로 응답한다.")
  @SecurityRequirement(name = "bearerAuth")
  @Parameter(
      in = ParameterIn.PATH,
      name = "animalId",
      required = true,
      description = "조회할 동물 ID",
      example = "1")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "해당 사용자가 소유한 동물이 없음 (ANIMAL_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<AnimalDetailResponse> getAnimal(UserPrincipal principal, Long animalId);

  @Operation(
      summary = "동물 삭제",
      description =
          "로그인한 사용자가 소유한 동물을 삭제한다."
              + "삭제된 동물은 조회와 농장 슬롯 배치에서 자동으로 제외된다. "
              + "이미 삭제되었거나 다른 사용자의 동물이면 404(ANIMAL_NOT_FOUND)로 응답한다.")
  @SecurityRequirement(name = "bearerAuth")
  @Parameter(
      in = ParameterIn.PATH,
      name = "animalId",
      required = true,
      description = "삭제할 동물 ID",
      example = "1")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "삭제 성공. data 는 null")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "해당 사용자가 소유한 동물이 없음 (ANIMAL_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<Void> deleteAnimal(UserPrincipal principal, Long animalId);

  @Operation(
      summary = "동물 슬롯 위치 교체",
      description =
          "이미 농장에 배치된 동물을 같은 테마 농장의 대상 좌표(층 번호, 슬롯 번호)로 이동한다. 대상 슬롯이 비어 있으면 "
              + "그대로 이동하고, 사용자의 다른 동물이 있으면 두 동물의 위치를 서로 교체한다. 농장은 동물의 카드 타입으로 "
              + "결정되므로 요청에 지정하지 않는다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이동 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "다른 사용자의 동물 접근 (FORBIDDEN_ANIMAL_ACCESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "동물 없음, 농장 없음, 또는 미개방 층이거나 범위를 벗어난 좌표 (FARM_SLOT_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description = "아직 농장에 배치되지 않은 동물 (ANIMAL_NOT_PLACED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<AnimalSlotMoveResponse> moveSlot(
      @Parameter(description = "이동할 동물 ID", example = "1") Long animalId,
      AnimalSlotMoveRequest request,
      UserPrincipal principal);
}
