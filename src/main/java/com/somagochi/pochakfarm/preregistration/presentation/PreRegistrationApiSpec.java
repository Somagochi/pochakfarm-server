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
      description = "출시 알림을 위한 사전예약을 등록한다. 휴대폰 번호당 1회만 등록 가능하며, 취소된 번호는 다시 등록할 수 있다.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "사전예약 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "잘못된 휴대폰 번호(INVALID_PHONE_NUMBER) 또는 필수 동의 누락(REQUIRED_CONSENT_REQUIRED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description = "이미 등록된 휴대폰 번호(PHONE_NUMBER_ALREADY_REGISTERED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<PreRegistrationResponse> register(PreRegistrationRequest request);

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
  ApiResponse<PreRegistrationResponse> cancel(String phoneNumber);
}
