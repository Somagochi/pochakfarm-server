package com.somagochi.pochakfarm.badge.presentation;

import com.somagochi.pochakfarm.badge.domain.BadgeCategory;
import com.somagochi.pochakfarm.badge.dto.OwnedBadgeResponse;
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

@Tag(name = "Badge", description = "뱃지 관련 API")
public interface BadgeApiSpec {
  @Operation(
      summary = "내 뱃지 보관함 조회",
      description =
          "로그인한 사용자가 획득한 뱃지를 커서 기반으로 조회한다(뱃지 id 오름차순, 한 페이지 20개). "
              + "category 를 지정하면 해당 카테고리(ACHIEVEMENT: 업적 뱃지, GYM_LEADER: 관장 뱃지)만, 생략하면 전체를 반환하며 대소문자를 구분하지 않는다. "
              + "cursor 에 이전 응답의 nextCursor(마지막 항목의 뱃지 id)를 넘기면 다음 페이지를 반환한다. "
              + "보유한 뱃지가 없으면 content 는 빈 배열이다. "
              + "각 항목에는 code, category, name, description, imageUrl, thumbnailImageUrl, acquiredAt(획득 일시)이 포함된다. "
              + "thumbnailImageUrl 은 관장 뱃지(GYM_LEADER)일 때 해당 관장의 썸네일 이미지이며, 업적 뱃지이거나 관장 썸네일이 없으면 null 이다. "
              + "이미지 키가 없으면 imageUrl은 null이며, 삭제된 뱃지와 획득 기록은 제외한다.")
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
  ApiResponse<CursorPage<OwnedBadgeResponse>> getOwnedBadges(
      @Parameter(description = "카테고리 필터. 생략하면 전체", example = "GYM_LEADER") BadgeCategory category,
      @Parameter(description = "다음 페이지 커서(이전 응답의 nextCursor). 첫 페이지는 생략", example = "20")
          Long cursor,
      UserPrincipal principal);
}
