package com.somagochi.pochakfarm.battle.presentation;

import com.somagochi.pochakfarm.battle.dto.BattleStartRequest;
import com.somagochi.pochakfarm.battle.dto.BattleStartResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderDetailResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderResponse;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Battle", description = "NPC 관장 대전 API")
public interface BattleApiSpec {

  @Operation(
      summary = "관장 목록 조회",
      description =
          "NPC 관장 8명을 도전 순서 오름차순으로 조회한다. "
              + "목록에는 썸네일과 클리어·해금 여부만 내려주고, "
              + "관장 코드·뱃지 코드·해금 조건 상세와 관장 동물은 관장 상세 API 로 조회한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<List<GymLeaderResponse>> getGymLeaders(UserPrincipal principal);

  @Operation(
      summary = "관장 상세 조회",
      description =
          "관장 1명과 관장 동물 3마리를 조회한다. "
              + "관장은 원본 이미지와 함께 요구 레벨·직전 관장 뱃지 두 해금 조건의 충족 여부를 각각 내려준다. "
              + "관장 동물은 이미지·이름·출전 순서·티어·타입만 공개하고 "
              + "스킬, 전투 유형, 발동 확률, 승부 포인트는 응답에 포함하지 않는다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "존재하지 않는 관장 (GYM_LEADER_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<GymLeaderDetailResponse> getGymLeader(
      @Parameter(description = "관장 ID", required = true, example = "4") Long gymLeaderId,
      UserPrincipal principal);

  @Operation(
      summary = "대전 시작",
      description =
          "내 동물 3마리와 출전 순서를 편성해 관장전을 시작한다. "
              + "서버가 관장 해금 조건(직전 관장 뱃지 + 요구 레벨)과 출전 동물의 휴식 여부를 다시 검증한다. "
              + "대전이 생성되면 출전 동물 3마리에 서버 시각 기준 30분 휴식이 걸리며, "
              + "대전 생성과 휴식 설정은 하나의 트랜잭션으로 처리된다. "
              + "clientRequestId 가 같은 재요청은 최초 생성 결과를 그대로 반환한다. "
              + "이미 클리어한 관장에도 다시 도전할 수 있다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "대전 생성 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "편성이 3마리가 아니거나 출전 순서·동물이 중복됨 (INVALID_BATTLE_ENTRY)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "해금 조건 미충족 관장 (GYM_LEADER_LOCKED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "존재하지 않는 관장 (GYM_LEADER_NOT_FOUND) 또는 본인 소유가 아닌 동물 (ANIMAL_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description =
          "휴식 중인 동물을 편성함 (BATTLE_ANIMAL_RESTING) 또는 진행 중인 대전이 있음 (BATTLE_ALREADY_IN_PROGRESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<BattleStartResponse> startBattle(BattleStartRequest request, UserPrincipal principal);
}
