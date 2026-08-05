package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.auth.application.TokenService;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.domain.WithdrawalReason;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WithdrawServiceTest {

  @Test
  void withdrawsUserAndRevokesTokens() {
    UserRepository userRepository = mock(UserRepository.class);
    TokenService tokenService = mock(TokenService.class);
    WithdrawService withdrawService = new WithdrawService(userRepository, tokenService);
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    withdrawService.withdraw(1L, "access-token", "refresh-token", WithdrawalReason.LOW_USAGE);

    assertTrue(user.isDeleted());
    assertEquals(WithdrawalReason.LOW_USAGE, user.getWithdrawalReason());
    verify(tokenService).revokeTokens("access-token", "refresh-token");
  }

  @Test
  void doesNotRevokeTokensWhenUserNotFound() {
    UserRepository userRepository = mock(UserRepository.class);
    TokenService tokenService = mock(TokenService.class);
    WithdrawService withdrawService = new WithdrawService(userRepository, tokenService);
    given(userRepository.findById(1L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                withdrawService.withdraw(
                    1L, "access-token", "refresh-token", WithdrawalReason.OTHER));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    verify(tokenService, never()).revokeTokens(any(), any());
  }

  @Test
  void withdrawsWithoutReasonForBackwardCompatibility() {
    UserRepository userRepository = mock(UserRepository.class);
    TokenService tokenService = mock(TokenService.class);
    WithdrawService withdrawService = new WithdrawService(userRepository, tokenService);
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    withdrawService.withdraw(1L, "access-token", "refresh-token", null);

    assertTrue(user.isDeleted());
    assertNull(user.getWithdrawalReason());
    verify(tokenService).revokeTokens("access-token", "refresh-token");
  }
}
