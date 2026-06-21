package com.somagochi.pochakfarm.auth.presentation;

import com.somagochi.pochakfarm.auth.application.SocialLoginService;
import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.application.LogoutService;
import com.somagochi.pochakfarm.auth.dto.LogoutRequest;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SocialLoginService socialLoginService;
    private final LogoutService logoutService;

  @PostMapping("/login")
  public ApiResponse<SocialLoginResponse> login(@RequestBody SocialLoginRequest request) {
    return ApiResponse.success(socialLoginService.login(request));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      Authentication authentication, @RequestBody LogoutRequest request) {
    logoutService.logout((String) authentication.getCredentials(), request.refreshToken());
    return ApiResponse.empty();
  }
}
