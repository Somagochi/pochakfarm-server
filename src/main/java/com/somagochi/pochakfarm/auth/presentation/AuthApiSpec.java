package com.somagochi.pochakfarm.auth.presentation;

import com.somagochi.pochakfarm.auth.dto.LogoutRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

@Tag(name = "Auth", description = "인증 관련 API")
public interface AuthApiSpec {

  @Operation(summary = "소셜 로그인", description = "소셜 provider 토큰으로 로그인하고 액세스/리프레시 토큰을 발급한다.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "지원하지 않는 소셜 provider",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "유효하지 않은 소셜 토큰",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<SocialLoginResponse> login(SocialLoginRequest request);

  @Operation(summary = "로그아웃", description = "액세스 토큰을 블랙리스트 처리하고 리프레시 토큰을 무효화한다.")
  @Parameter(
      in = ParameterIn.HEADER,
      name = "Authorization",
      required = true,
      description = "액세스 토큰 (형식: `Bearer {accessToken}`)",
      example = "Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "로그아웃 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<Void> logout(Authentication authentication, LogoutRequest request);
}
