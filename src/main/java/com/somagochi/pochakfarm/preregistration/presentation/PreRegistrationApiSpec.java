package com.somagochi.pochakfarm.preregistration.presentation;

import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationCouponSmsRequest;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationCouponSmsResult;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationRequest;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Tag(name = "PreRegistration", description = "사전예약 API")
public interface PreRegistrationApiSpec {

  @Operation(
      summary = "사전예약 등록",
      description =
          "출시 알림을 위한 사전예약을 등록한다. 휴대폰 번호당 1회만 등록 가능하며, "
              + "취소된 번호는 다시 등록할 수 있다. 요청 본문에 characterizationId를 함께 전달한다.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "사전예약 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description =
          "잘못된 휴대폰 번호(INVALID_PHONE_NUMBER), 유효하지 않은 캐릭터라이징 ID"
              + "(INVALID_CHARACTERIZATION_ID), 또는 필수 동의 누락(REQUIRED_CONSENT_REQUIRED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description = "이미 등록된 휴대폰 번호(PHONE_NUMBER_ALREADY_REGISTERED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "캐릭터라이징 내역 없음(CHARACTERIZATION_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @Deprecated
  ApiResponse<PreRegistrationResponse> register(PreRegistrationRequest request)
      throws NoResourceFoundException;

  @Operation(
      summary = "사전예약 취소",
      description = "휴대폰 번호 기준으로 사전예약을 취소(soft delete)한다. 이미 취소된 경우에도 멱등하게 성공한다.")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "phoneNumber",
      required = true,
      description = "사전예약한 휴대폰 번호")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "취소 성공 (status=CANCELED)")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "사전예약 내역 없음(PRE_REGISTRATION_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @Deprecated
  ApiResponse<PreRegistrationResponse> cancel(String phoneNumber) throws NoResourceFoundException;

  @Operation(
      summary = "사전예약자 쿠폰 SMS 일괄 발송",
      description =
          "쿠폰이 발급된 사전예약자 중 아직 SMS를 받지 않은 대상 전원에게 쿠폰 코드를 문자로 발송한다. "
              + "요청 본문의 messageTemplate에 {couponCode} 플레이스홀더를 포함해야 하며, 수신자별 쿠폰 코드로 치환된다. "
              + "dryRun=true(기본값)면 발송 없이 대상 수만 집계한다. 발송 성공 건은 재호출해도 다시 발송되지 않는다.")
  @Parameter(
      in = ParameterIn.HEADER,
      name = "X-Admin-Key",
      required = true,
      description = "관리용 API 키")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "dryRun",
      description = "true면 발송하지 않고 대상 수만 반환 (기본값 true)")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "발송 결과 (targetCount/sentCount/failedCount/couponNotIssuedCount)")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "messageTemplate 누락 또는 {couponCode} 플레이스홀더 없음(INVALID_PARAMETER)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "관리용 API 키 불일치(FORBIDDEN_ADMIN_ACCESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<PreRegistrationCouponSmsResult> sendCouponSms(
      String adminKey, boolean dryRun, PreRegistrationCouponSmsRequest request);
}
