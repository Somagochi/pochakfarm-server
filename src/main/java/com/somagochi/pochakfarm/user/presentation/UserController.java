package com.somagochi.pochakfarm.user.presentation;

import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.user.application.UserNicknameService;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import com.somagochi.pochakfarm.user.application.UserTermsAgreementService;
import com.somagochi.pochakfarm.user.application.WithdrawService;
import com.somagochi.pochakfarm.user.dto.NicknameResponse;
import com.somagochi.pochakfarm.user.dto.NicknameUpdateRequest;
import com.somagochi.pochakfarm.user.dto.TermsAgreementRequest;
import com.somagochi.pochakfarm.user.dto.UserProfileResponse;
import com.somagochi.pochakfarm.user.dto.UserResponse;
import com.somagochi.pochakfarm.user.dto.WithdrawRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApiSpec {

  private final UserQueryService userQueryService;
  private final UserNicknameService userNicknameService;
  private final UserTermsAgreementService userTermsAgreementService;
  private final WithdrawService withdrawService;

  @Override
  @GetMapping("/me")
  public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(userQueryService.getMe(principal.id()));
  }

  @Override
  @PatchMapping("/me/nickname")
  public ApiResponse<NicknameResponse> changeNickname(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestBody NicknameUpdateRequest request) {
    return ApiResponse.success(
        userNicknameService.changeNickname(principal.id(), request.nickname()));
  }

  @Override
  @PostMapping("/me/terms-agreement")
  public ApiResponse<Void> agreeToTerms(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestBody TermsAgreementRequest request) {
    userTermsAgreementService.agree(principal.id(), request);
    return ApiResponse.empty();
  }

  @Override
  @GetMapping("/nickname/check")
  public ApiResponse<Void> checkNickname(@RequestParam String nickname) {
    userNicknameService.checkNickname(nickname);
    return ApiResponse.empty();
  }

  @Override
  @DeleteMapping("/me")
  public ApiResponse<Void> withdraw(
      Authentication authentication, @RequestBody WithdrawRequest request) {
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    withdrawService.withdraw(
        principal.id(),
        (String) authentication.getCredentials(),
        request.refreshToken(),
        request.withdrawalReason());
    return ApiResponse.empty();
  }

  @Override
  @GetMapping("/profile")
  public ApiResponse<UserProfileResponse> getProfile(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(userQueryService.getProfile(principal.id()));
  }
}
