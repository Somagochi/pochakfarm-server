package com.somagochi.pochakfarm.user.presentation;

import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.user.dto.NicknameResponse;
import com.somagochi.pochakfarm.user.dto.NicknameUpdateRequest;
import com.somagochi.pochakfarm.user.dto.UserProfileResponse;
import com.somagochi.pochakfarm.user.dto.UserResponse;
import com.somagochi.pochakfarm.user.dto.WithdrawRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

@Tag(name = "User", description = "회원 관련 API")
public interface UserApiSpec {

  @Operation(summary = "내 가입정보 조회", description = "액세스 토큰으로 인증된 사용자의 가입정보(이메일, 소셜 연동, 닉네임)를 조회한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "회원을 찾을 수 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<UserResponse> getMe(UserPrincipal principal);

  @Operation(summary = "내 프로필 조회", description = "액세스 토큰으로 인증된 사용자의 게임 프로필(닉네임, 레벨, 코인)을 조회한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "회원을 찾을 수 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<UserProfileResponse> getProfile(UserPrincipal principal);

  @Operation(summary = "닉네임 변경", description = "현재 로그인한 회원의 닉네임을 변경한다.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "유효하지 않은 닉네임",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "회원을 찾을 수 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<NicknameResponse> changeNickname(
      UserPrincipal principal, NicknameUpdateRequest request);

  @Operation(summary = "회원 탈퇴", description = "현재 로그인한 회원을 탈퇴(소프트 삭제)하고 액세스/리프레시 토큰을 무효화한다.")
  @SecurityRequirement(name = "bearerAuth")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 성공")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "401",
      description = "인증 실패 (토큰 만료/무효 또는 토큰 소유자 불일치)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "회원을 찾을 수 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  ApiResponse<Void> withdraw(Authentication authentication, WithdrawRequest request);
}
