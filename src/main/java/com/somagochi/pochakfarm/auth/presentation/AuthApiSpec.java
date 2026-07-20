package com.somagochi.pochakfarm.auth.presentation;

import com.somagochi.pochakfarm.auth.dto.LogoutRequest;
import com.somagochi.pochakfarm.auth.dto.RefreshRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

@Tag(name = "Auth", description = "인증 관련 API")
public interface AuthApiSpec {

  @Operation(
      summary = "소셜 로그인 (App SDK 방식)",
      description =
          "App SDK로 발급받은 provider 토큰으로 로그인하고 서비스 access/refresh 토큰을 발급한다.<br>"
              + "- provider: kakao / naver / apple (대소문자 무관)<br>"
              + "- token: 카카오·네이버는 access token, 애플은 id token(JWT)<br>"
              + "REST(OAuth2 리다이렉트) 방식은 `GET /api/auth/oauth2/{provider}` 참고.")
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

  @Operation(
      summary = "토큰 재발급",
      description = "리프레시 토큰으로 새로운 액세스/리프레시 토큰을 발급한다. 기존 리프레시 토큰은 회전(rotate)되어 무효화된다.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "토큰 재발급 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "유효하지 않은 리프레시 토큰 (만료/무효/타입 불일치/회전됨)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<TokenResponse> refresh(RefreshRequest request);

  @Operation(summary = "로그아웃", description = "액세스 토큰을 블랙리스트 처리하고 리프레시 토큰을 무효화한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "로그아웃 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<Void> logout(Authentication authentication, LogoutRequest request);
}
