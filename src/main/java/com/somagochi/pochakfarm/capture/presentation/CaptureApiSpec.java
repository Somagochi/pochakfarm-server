package com.somagochi.pochakfarm.capture.presentation;

import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Capture", description = "앱 포착 API")
public interface CaptureApiSpec {

  @Operation(
      summary = "포착 시작",
      description =
          "일일 포착 횟수를 확인하고 사용자 레벨 기반 티어와 동물 타입을 결정한다. "
              + "Capture와 원본 이미지 업로드용 presigned PUT URL을 반환한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "포착 시작 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description =
          "잘못된 clientRequestId(INVALID_CLIENT_REQUEST_ID) 또는 "
              + "지원하지 않는 이미지 형식(UNSUPPORTED_CONTENT_TYPE)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "사용자 없음(USER_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description =
          "일일 포착 횟수 소진(CAPTURE_ATTEMPT_EXHAUSTED) 또는 "
              + "동일 clientRequestId 요청 내용 충돌(CAPTURE_REQUEST_CONFLICT)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CaptureStartResponse> startCapture(
      @Schema(hidden = true) UserPrincipal principal, CaptureStartRequest request);
}
