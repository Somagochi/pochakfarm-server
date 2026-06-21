package com.somagochi.pochakfarm.auth.application;

import com.somagochi.pochakfarm.auth.service.TokenService;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.jwt.JwtPayload;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService {

  private final TokenService tokenService;

  public void logout(String accessToken, String refreshToken) {
    JwtPayload accessPayload = tokenService.parseAccessToken(accessToken);
    JwtPayload refreshPayload = tokenService.parseRefreshToken(refreshToken);
    if (!accessPayload.subject().equals(refreshPayload.subject())) {
      throw new JwtAuthenticationException(ErrorCode.TOKEN_OWNER_MISMATCH);
    }
    tokenService.revokeRefreshToken(refreshToken);
    tokenService.blacklistToken(accessToken);
  }
}
