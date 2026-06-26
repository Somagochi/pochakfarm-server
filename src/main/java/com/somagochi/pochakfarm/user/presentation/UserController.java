package com.somagochi.pochakfarm.user.presentation;

import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.user.application.UserService;
import com.somagochi.pochakfarm.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApiSpec {

  private final UserService userService;

  @Override
  @GetMapping("/me")
  public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(userService.getProfile(principal.id()));
  }
}
