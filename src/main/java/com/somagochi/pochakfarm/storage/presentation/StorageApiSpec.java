package com.somagochi.pochakfarm.storage.presentation;

import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.storage.dto.ConfirmRequest;
import com.somagochi.pochakfarm.storage.dto.ConfirmResponse;
import com.somagochi.pochakfarm.storage.dto.PresignRequest;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

@Tag(name = "Storage", description = "이미지 업로드 API")
public interface StorageApiSpec {

  @Operation(
      summary = "업로드 presigned URL 발급",
      description = "S3에 직접 업로드할 수 있는 presigned PUT URL과 객체 key를 발급한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description =
          "지원하지 않는 contentType(UNSUPPORTED_CONTENT_TYPE) 또는 잘못된 업로드 용도(INVALID_UPLOAD_PURPOSE)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<PresignResponse> presign(Authentication authentication, PresignRequest request);

  @Operation(
      summary = "업로드 확정",
      description = "presigned URL로 업로드한 객체의 소유권과 메타데이터를 검증하고 최종 접근 URL을 반환한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확정 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "지원하지 않는 contentType(UNSUPPORTED_CONTENT_TYPE) 또는 파일 용량 초과(FILE_TOO_LARGE)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "본인 소유가 아닌 객체 접근 (FORBIDDEN_FILE_ACCESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<ConfirmResponse> confirm(Authentication authentication, ConfirmRequest request);
}
