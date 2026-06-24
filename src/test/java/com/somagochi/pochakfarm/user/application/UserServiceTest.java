package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserServiceTest {

  @Test
  void withdrawMarksUserDeleted() {
    UserRepository userRepository = mock(UserRepository.class);
    UserService userService = new UserService(userRepository);
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    userService.withdraw(1L);

    assertTrue(user.isDeleted());
  }

  @Test
  void withdrawThrowsWhenUserNotFound() {
    UserRepository userRepository = mock(UserRepository.class);
    UserService userService = new UserService(userRepository);
    given(userRepository.findById(1L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> userService.withdraw(1L));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }
}
