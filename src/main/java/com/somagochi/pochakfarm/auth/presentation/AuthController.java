package com.somagochi.pochakfarm.auth.presentation;

import com.somagochi.pochakfarm.auth.application.LogoutService;
import com.somagochi.pochakfarm.auth.application.RefreshService;
import com.somagochi.pochakfarm.auth.application.SocialLoginService;
import com.somagochi.pochakfarm.auth.dto.LogoutRequest;
import com.somagochi.pochakfarm.auth.dto.RefreshRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
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
public class AuthController implements AuthApiSpec {

  private final SocialLoginService socialLoginService;
  private final LogoutService logoutService;
  private final RefreshService refreshService;

  @Override
  @PostMapping("/login")
  public ApiResponse<SocialLoginResponse> login(@RequestBody SocialLoginRequest request) {
    return ApiResponse.success(socialLoginService.login(request));
  }

  @Override
  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> refresh(@RequestBody RefreshRequest request) {
    return ApiResponse.success(refreshService.refresh(request.refreshToken()));
  }

  @Override
  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      Authentication authentication, @RequestBody LogoutRequest request) {
    logoutService.logout((String) authentication.getCredentials(), request.refreshToken());
    return ApiResponse.empty();
  }
}
