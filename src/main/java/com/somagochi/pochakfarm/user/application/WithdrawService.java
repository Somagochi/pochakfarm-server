package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.auth.application.TokenService;
import org.springframework.stereotype.Service;

@Service
public class WithdrawService {

  private final UserService userService;
  private final TokenService tokenService;

  public WithdrawService(UserService userService, TokenService tokenService) {
    this.userService = userService;
    this.tokenService = tokenService;
  }

  public void withdraw(Long userId, String accessToken, String refreshToken) {
    userService.withdraw(userId);
    tokenService.revokeTokens(accessToken, refreshToken);
  }
}
