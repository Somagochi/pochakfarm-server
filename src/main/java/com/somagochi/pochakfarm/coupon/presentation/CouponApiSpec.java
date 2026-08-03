package com.somagochi.pochakfarm.coupon.presentation;

import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.coupon.dto.CouponCompleteRequest;
import com.somagochi.pochakfarm.coupon.dto.CouponCompleteResponse;
import com.somagochi.pochakfarm.coupon.dto.CouponRedeemRequest;
import com.somagochi.pochakfarm.coupon.dto.CouponRedeemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Coupon", description = "쿠폰 관련 API")
public interface CouponApiSpec {

  @Operation(
      summary = "사전예약 쿠폰 입력 (카드 이관)",
      description =
          "사전예약 쿠폰 코드를 검증하고 사전예약 때 만든 캐릭터 카드를 S 티어 포획 카드로 이관한다. "
              + "응답에 카드 정보와 누끼(동물) 이미지 업로드용 presigned URL 이 포함된다. "
              + "같은 유저가 같은 쿠폰으로 재호출하면 이미 이관된 카드를 그대로 반환한다. "
              + "이 단계에서는 쿠폰이 사용 처리되지 않으며, 사용 처리는 완료 API 에서 이뤄진다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이관 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "만료된 쿠폰",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "존재하지 않는 쿠폰 코드",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description = "이미 사용된 쿠폰 또는 이미 쿠폰을 사용한 유저",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CouponRedeemResponse> redeem(UserPrincipal principal, CouponRedeemRequest request);

  @Operation(
      summary = "사전예약 쿠폰 사용 완료",
      description =
          "클라이언트가 누끼(동물) 이미지를 업로드한 뒤 호출한다. "
              + "이관된 카드에 누끼 이미지를 반영하고 코인을 지급하며 쿠폰을 사용 처리한다. "
              + "쿠폰 입력(이관) API 를 먼저 호출한 유저만 완료할 수 있다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 완료")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "다른 유저에게 이관된 쿠폰",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "존재하지 않는 쿠폰 코드 또는 업로드되지 않은 이미지",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description = "이미 사용된 쿠폰 또는 이관이 시작되지 않은 쿠폰",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CouponCompleteResponse> complete(
      UserPrincipal principal, CouponCompleteRequest request);
}
