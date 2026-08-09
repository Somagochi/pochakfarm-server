package com.somagochi.pochakfarm.achievement.presentation;

import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.dto.AchievementClaimResponse;
import com.somagochi.pochakfarm.achievement.dto.AchievementResponse;
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

@Tag(name = "Achievement", description = "업적 관련 API")
public interface AchievementApiSpec {

  @Operation(
      summary = "업적 목록 조회",
      description =
          "로그인한 사용자의 업적 목록을 진행도와 함께 커서 기반으로 조회한다(업적 id 오름차순, 한 페이지 20개). "
              + "category 를 지정하면 해당 카테고리만, cursor 에 이전 응답의 nextCursor(마지막 항목의 id)를 넘기면 다음 페이지를 반환한다. "
              + "진행도는 저장값이 아니라 조회 시점에 동물 보유/농장 배치/사전예약 쿠폰 사용 이력에서 다시 계산하며, 목표에 도달했는데 달성 기록이 없으면 그 자리에서 달성으로 확정한다. "
              + "달성 판정은 현재 페이지가 아니라 전체 업적을 대상으로 하므로, 첫 페이지만 조회해도 뒤 페이지 업적의 달성이 누락되지 않는다. "
              + "progress.current 는 목표를 넘어도 실제값을 그대로 반환하므로 진행률 표시에는 progress.target 으로 잘라 쓴다. "
              + "imageUrl 은 달성 여부에 따라 미달성/달성 이미지가 선택되어 내려가며 등록되지 않은 경우 응답에서 빠진다. "
              + "achieved 가 true 인 항목에만 achievedInfo(achievedAt, rewardClaimed) 객체가 포함되며, rewardClaimed 가 false 면 보상을 수령할 수 있다. "
              + "숨김 업적은 달성 전에는 hidden=true 에 code/category 만 내려가는 잠금 상태로 표시되며, 달성 후에는 모두 공개된다. "
              + "노출이 중단된 업적은 목록에서 빠지지만 이미 달성한 사용자에게는 계속 보인다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "지원하지 않는 카테고리 값",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "사용자 없음 (USER_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CursorPage<AchievementResponse>> getAchievements(
      @Parameter(description = "카테고리 필터. 생략하면 전체", example = "FARM") AchievementCategory category,
      @Parameter(description = "다음 페이지 커서(이전 응답의 nextCursor). 첫 페이지는 생략", example = "20")
          Long cursor,
      UserPrincipal principal);

  @Operation(
      summary = "업적 보상 수령",
      description =
          "달성한 업적의 보상을 수령한다. 수령은 업적 단위이며 그 업적에 걸린 보상(코인/경험치/뱃지)을 한 트랜잭션에서 모두 지급한다. "
              + "업적 목록을 조회하지 않고 바로 호출해도 되며, 달성 기록이 없으면 이 시점에 다시 판정한다. "
              + "이미 보유한 뱃지는 중복 지급되지 않고 조용히 넘어간다. "
              + "응답의 coins/experience 는 지급 후 갱신된 사용자 잔액이다.")
  @SecurityRequirement(name = "bearerAuth")
  @Parameter(
      in = ParameterIn.PATH,
      name = "code",
      required = true,
      description = "수령할 업적 코드",
      example = "ACH002")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수령 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "아직 달성하지 않은 업적 (ACHIEVEMENT_NOT_ACHIEVED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "존재하지 않는 업적 코드 (ACHIEVEMENT_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description = "이미 수령한 보상 (ACHIEVEMENT_REWARD_ALREADY_CLAIMED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<AchievementClaimResponse> claimReward(UserPrincipal principal, String code);
}
