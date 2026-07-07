package com.somagochi.pochakfarm.user.application;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.auth.application.TokenService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class WithdrawServiceTest {

  @Test
  void withdrawsUserThenRevokesSession() {
    UserService userService = mock(UserService.class);
    TokenService tokenService = mock(TokenService.class);
    WithdrawService withdrawService = new WithdrawService(userService, tokenService);

    withdrawService.withdraw(1L, "access-token", "refresh-token");

    InOrder inOrder = inOrder(userService, tokenService);
    inOrder.verify(userService).withdraw(1L);
    inOrder.verify(tokenService).revokeTokens("access-token", "refresh-token");
  }
}
