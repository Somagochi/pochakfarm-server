package com.somagochi.pochakfarm.capture.presentation;

import com.somagochi.pochakfarm.capture.dto.CaptureCompleteResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureResponse;
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

  @Operation(summary = "원본 이미지 업로드 완료", description = "S3 원본 업로드를 검증하고 비동기 AI 이미지 생성을 접수한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "202",
      description = "생성 접수 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "Capture 소유권 없음(FORBIDDEN_CAPTURE_ACCESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "Capture 없음(CAPTURE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CaptureCompleteResponse> completeOriginalImage(
      @Schema(hidden = true) UserPrincipal principal, Long captureId);

  @Operation(
      summary = "미니게임 결과 제출",
      description =
          "클라이언트가 확정한 최대 3회의 투척 성공 여부를 제출하고 최종 게임 상태와 보상을 확정한다. "
              + "최초 요청에는 경험치 반영 전후 progression을 반환한다. "
              + "재요청에는 최초 지급 경험치와 조회 시점의 최신 progression.after를 반환하며 before는 null이다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "게임 결과 확정 또는 최초 확정 결과 재반환")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "잘못된 투척 기록(INVALID_GAME_RESULT)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "Capture 소유권 없음(FORBIDDEN_CAPTURE_ACCESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "Capture 없음(CAPTURE_NOT_FOUND) 또는 사용자 없음(USER_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CaptureGameResultResponse> submitGameResult(
      @Schema(hidden = true) UserPrincipal principal,
      Long captureId,
      CaptureGameResultRequest request);

  @Operation(summary = "포착 상태 및 생성 결과 조회")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "Capture 소유권 없음(FORBIDDEN_CAPTURE_ACCESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "Capture 없음(CAPTURE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CaptureResponse> getCapture(
      @Schema(hidden = true) UserPrincipal principal, Long captureId);
}
