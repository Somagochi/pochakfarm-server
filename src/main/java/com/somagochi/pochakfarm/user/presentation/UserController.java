package com.somagochi.pochakfarm.user.presentation;

import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.user.application.ChangeNicknameService;
import com.somagochi.pochakfarm.user.application.UserService;
import com.somagochi.pochakfarm.user.application.WithdrawService;
import com.somagochi.pochakfarm.user.dto.NicknameResponse;
import com.somagochi.pochakfarm.user.dto.NicknameUpdateRequest;
import com.somagochi.pochakfarm.user.dto.UserResponse;
import com.somagochi.pochakfarm.user.dto.WithdrawRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApiSpec {

  private final UserService userService;
  private final ChangeNicknameService changeNicknameService;
  private final WithdrawService withdrawService;

  @Override
  @GetMapping("/me")
  public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(userService.getProfile(principal.id()));
  }

  @Override
  @PatchMapping("/me/nickname")
  public ApiResponse<NicknameResponse> changeNickname(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestBody NicknameUpdateRequest request) {
    return ApiResponse.success(
        changeNicknameService.changeNickname(principal.id(), request.nickname()));
  }

  @Override
  @DeleteMapping("/me")
  public ApiResponse<Void> withdraw(
      Authentication authentication, @RequestBody WithdrawRequest request) {
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    withdrawService.withdraw(
        principal.id(), (String) authentication.getCredentials(), request.refreshToken());
    return ApiResponse.empty();
  }
}
