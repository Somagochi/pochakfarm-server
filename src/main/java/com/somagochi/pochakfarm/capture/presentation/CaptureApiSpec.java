package com.somagochi.pochakfarm.capture.presentation;

import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureCompleteResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
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
      description = "동일 clientRequestId 요청 내용 충돌(CAPTURE_REQUEST_CONFLICT)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "402",
      description = "코인 결제 동의 필요(COIN_PAYMENT_REQUIRED) 또는 코인 부족(INSUFFICIENT_COINS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CaptureStartResponse> startCapture(
      @Schema(hidden = true) UserPrincipal principal, CaptureStartRequest request);

  @Operation(
      summary = "포착 가능 상태 조회",
      description = "오늘 남은 무료 포착 횟수, 다음 초기화 시각, 보유 코인, 추가 포착 비용과 시작 가능 여부를 조회한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "사용자 없음(USER_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CaptureAvailabilityResponse> getAvailability(
      @Schema(hidden = true) UserPrincipal principal);

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

  @Operation(
      summary = "농장용 누끼 이미지 업로드 URL 발급",
      description = "이미지 생성과 미니게임에 성공한 일반 포착의 PNG 업로드용 presigned PUT URL을 발급한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "Capture 소유권 없음(FORBIDDEN_CAPTURE_ACCESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "Capture 없음(CAPTURE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description = "저장 불가능 상태 또는 이미 배치됨(CAPTURE_NOT_PLACEABLE, CAPTURE_ALREADY_PLACED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<PresignResponse> presignAnimalImage(
      @Schema(hidden = true) UserPrincipal principal, Long captureId);

  @Operation(
      summary = "포착 동물 농장 저장",
      description = "누끼 PNG를 등록하고 Capture 타입 농장의 선택 슬롯에 동물을 저장하거나 명시한 기존 동물을 교체한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "저장 성공 또는 동일 요청 결과 재반환")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "누끼 이미지 형식 오류(UNSUPPORTED_CONTENT_TYPE)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "Capture 또는 교체 동물 소유권 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description =
          "Capture, 업로드 이미지 또는 농장 슬롯 없음"
              + "(CAPTURE_NOT_FOUND, FILE_NOT_FOUND, FARM_SLOT_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description = "배치 상태, 슬롯 점유, 교체 대상 또는 멱등 요청 충돌",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "413",
      description = "누끼 이미지 용량 초과(FILE_TOO_LARGE)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<CaptureAnimalPlacementResponse> placeAnimal(
      @Schema(hidden = true) UserPrincipal principal,
      Long captureId,
      CaptureAnimalPlacementRequest request);
}
