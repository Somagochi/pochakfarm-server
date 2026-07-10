package com.somagochi.pochakfarm.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService {

  private final TokenService tokenService;

  public void logout(String accessToken, String refreshToken) {
    tokenService.revokeTokens(accessToken, refreshToken);
  }
}
