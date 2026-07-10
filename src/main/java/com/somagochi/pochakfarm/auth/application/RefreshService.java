package com.somagochi.pochakfarm.auth.application;

import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.jwt.JwtPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshService {

  private final TokenService tokenService;

  public TokenResponse refresh(String refreshToken) {
    JwtPayload payload = tokenService.parseRefreshToken(refreshToken);
    return tokenService.refreshTokens(payload);
  }
}
