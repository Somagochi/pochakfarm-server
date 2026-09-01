package com.somagochi.pochakfarm.battle.presentation;

import com.somagochi.pochakfarm.battle.dto.BattleActionRequest;
import com.somagochi.pochakfarm.battle.dto.BattleActionResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultRequest;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundStartResponse;
import com.somagochi.pochakfarm.battle.dto.BattleStateResponse;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Battle Action", description = "관장 대전 스킬 선택 행동 / 진행 상태 복원 API")
public interface BattleActionApiSpec {

  @Operation(
      summary = "스킬 선택 행동",
      description =
          "한 번의 스킬 선택 행동을 서버가 판정한다. 경기당 동물 3마리 x 3회로 총 9회의 행동이 발생하며 actionSeq 는 1부터 9까지 순서대로 보내야 한다. "
              + "요청 순번이 다음 행동 순번과 다르면 409(BATTLE_ACTION_SEQUENCE_MISMATCH)로 거절한다. "
              + "skill 을 null 로 보내면 제한 시간 안에 선택하지 못한 미선택으로 처리해 유저 획득 승부 포인트를 0으로 판정하며, "
              + "선택했지만 발동에 실패한 경우와는 status 값(NOT_SELECTED / FAILED)으로 구분한다. "
              + "선택 마감 시각은 서버 시각 기준이며, 마감 이후에 스킬을 담아 보내면 409(BATTLE_ACTION_SELECTION_CLOSED)로 거절한다. "
              + "NPC 스킬은 행동 시작 시점의 승부 바 위치와 NPC 스킬 2개만으로 서버가 결정하며 유저 선택은 입력으로 사용하지 않는다. "
              + "양쪽 스킬은 동시에 판정하고 획득 포인트를 상계한 순승부 포인트만큼 승부 바를 한 번 이동한다. "
              + "동물의 3회 행동이 끝나면 승부 바 위치를 유지한 채 다음 동물로 교체하며, 교체 시 티어와 타입 상성 우위를 함께 반영한다. "
              + "승부 바가 최소·최댓값에 도달하면 그 즉시 경기를 종료하고 남은 행동과 동물 교체는 진행하지 않는다. "
              + "같은 actionSeq 로 다시 요청하면 최초 판정 결과를 그대로 반환한다.")
  @SecurityRequirement(name = "bearerAuth")
  @Parameter(
      in = ParameterIn.PATH,
      name = "battleId",
      required = true,
      description = "진행 중인 대전 ID",
      example = "1")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "판정 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "출전 동물이 보유하지 않은 스킬이거나 출전 정보가 없음 (INVALID_BATTLE_ENTRY)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "다른 사용자의 대전 (FORBIDDEN_BATTLE_ACCESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "대전을 찾을 수 없음 (BATTLE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description =
          "행동 순번 불일치 (BATTLE_ACTION_SEQUENCE_MISMATCH), 종료된 대전 (BATTLE_NOT_IN_PROGRESS), "
              + "선택 마감 이후 도착 (BATTLE_ACTION_SELECTION_CLOSED), 동시 요청 충돌 (BATTLE_ACTION_CONFLICT)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<BattleActionResponse> selectSkill(
      UserPrincipal principal, Long battleId, BattleActionRequest request);

  @Operation(
      summary = "대전 진행 상태 조회",
      description =
          "재접속 시 대전 화면을 복원하기 위한 진행 상태를 조회한다. "
              + "현재 출전 중인 동물, 진행된 행동 횟수와 다음 행동 순번, 승부 바 위치와 정책상 최소·최댓값, "
              + "지금까지 발생한 중계 이벤트 전체를 반환한다. "
              + "관장 동물의 스킬은 대전 전에 공개하지 않는 정책이라 npcEntry.skills 는 항상 null 이며, "
              + "NPC 가 사용한 스킬은 중계 이벤트로만 공개된다.")
  @SecurityRequirement(name = "bearerAuth")
  @Parameter(
      in = ParameterIn.PATH,
      name = "battleId",
      required = true,
      description = "조회할 대전 ID",
      example = "1")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "다른 사용자의 대전 (FORBIDDEN_BATTLE_ACCESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "대전을 찾을 수 없음 (BATTLE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<BattleStateResponse> getBattle(UserPrincipal principal, Long battleId);

  @Operation(
      summary = "최종 승부 시작",
      description =
          "9회 대전 후 동점 또는 1~2 승부 포인트 열세인 경기의 최종 승부를 시작한다. "
              + "클라이언트가 연출 준비를 마친 후 호출하며, 최초 호출에서만 3초 입력 타이머를 시작한다. "
              + "재호출해도 종료 시각은 연장되지 않으며, 최종 승부 대기 후 30초 이내에 시작하지 않으면 패배한다.")
  @SecurityRequirement(name = "bearerAuth")
  ApiResponse<BattleFinalRoundStartResponse> startFinalRound(
      UserPrincipal principal, Long battleId);

  @Operation(
      summary = "최종 승부 결과 제출",
      description =
          "3초간 집계한 tapCount 를 제출하면 서버가 0~3 승부 포인트로 환산하고 최종 승패를 판정한다. "
              + "입력 종료 후 통신 전송을 위한 1초 유예만 허용하며, 이후 요청은 시간 초과 패배로 처리한다.")
  @SecurityRequirement(name = "bearerAuth")
  ApiResponse<BattleFinalRoundResultResponse> submitFinalRound(
      UserPrincipal principal, Long battleId, BattleFinalRoundResultRequest request);
}
