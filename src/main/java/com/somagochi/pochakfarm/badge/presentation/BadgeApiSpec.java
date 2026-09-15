package com.somagochi.pochakfarm.badge.presentation;

import com.somagochi.pochakfarm.badge.dto.OwnedBadgeResponse;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Badge", description = "뱃지 관련 API")
public interface BadgeApiSpec {
  @Operation(
      summary = "내 뱃지 보관함 조회",
      description =
          "로그인한 사용자가 획득한 뱃지 전체를 코드 오름차순으로 반환한다. "
              + "페이지네이션 없이 data 배열로 반환하며, 보유한 뱃지가 없으면 빈 배열을 반환한다. "
              + "각 항목에는 code, name, description, imageUrl, acquiredAt(획득 일시)이 포함된다. "
              + "이미지 키가 없으면 imageUrl은 null이며, 삭제된 뱃지와 획득 기록은 제외한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<List<OwnedBadgeResponse>> getOwnedBadges(UserPrincipal principal);
}
