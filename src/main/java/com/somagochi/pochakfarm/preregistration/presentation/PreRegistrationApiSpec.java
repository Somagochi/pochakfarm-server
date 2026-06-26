package com.somagochi.pochakfarm.preregistration.presentation;

import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationRequest;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "PreRegistration", description = "사전예약 API")
public interface PreRegistrationApiSpec {

  @Operation(
      summary = "사전예약 등록",
      description =
          "출시 알림을 위한 사전예약을 등록한다. deviceToken 쿠키가 필요하며, 카드 생성을 완료한 기기만 등록할 수 있다. "
              + "휴대폰 번호당 1회, 기기당 1회만 등록 가능하다.")
  @Parameter(
      in = ParameterIn.COOKIE,
      name = "deviceToken",
      required = true,
      description = "카드 생성 시 발급된 익명 디바이스 토큰 쿠키")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "사전예약 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "잘못된 휴대폰 번호(INVALID_PHONE_NUMBER) 또는 필수 동의 누락(REQUIRED_CONSENT_REQUIRED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "카드를 생성하지 않았거나 유효하지 않은 기기 (DEVICE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description =
          "이미 등록된 휴대폰 번호(PHONE_NUMBER_ALREADY_REGISTERED) 또는 이미 등록한 기기(DEVICE_ALREADY_REGISTERED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<PreRegistrationResponse> register(String deviceToken, PreRegistrationRequest request);

  @Operation(
      summary = "사전예약 취소",
      description =
          "deviceToken 쿠키 기준으로 해당 기기의 사전예약을 취소(soft delete)한다. 이미 취소된 경우에도 멱등하게 성공하며, "
              + "취소 후에는 동일 기기/번호로 다시 사전예약할 수 있다.")
  @Parameter(
      in = ParameterIn.COOKIE,
      name = "deviceToken",
      required = true,
      description = "사전예약 시 사용한 익명 디바이스 토큰 쿠키")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "취소 성공 (status=CANCELED)")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "유효하지 않은 기기(DEVICE_NOT_FOUND) 또는 사전예약 내역 없음(PRE_REGISTRATION_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<PreRegistrationResponse> cancel(String deviceToken);
}
