package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.auth.application.TokenService;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.transaction.AfterCommitExecutor;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawService {

  private final UserRepository userRepository;
  private final TokenService tokenService;

  public WithdrawService(UserRepository userRepository, TokenService tokenService) {
    this.userRepository = userRepository;
    this.tokenService = tokenService;
  }

  @Transactional
  public void withdraw(Long userId, String accessToken, String refreshToken) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.withdraw();
    AfterCommitExecutor.execute(() -> tokenService.revokeTokens(accessToken, refreshToken));
  }
}
