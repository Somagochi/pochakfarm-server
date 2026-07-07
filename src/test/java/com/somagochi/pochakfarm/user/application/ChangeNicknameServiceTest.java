package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.NicknameResponse;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChangeNicknameServiceTest {

  @Test
  void changeNicknameUpdatesNickname() {
    UserRepository userRepository = mock(UserRepository.class);
    ChangeNicknameService changeNicknameService = new ChangeNicknameService(userRepository);
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    NicknameResponse response = changeNicknameService.changeNickname(1L, "포착이");

    assertEquals("포착이", response.nickname());
    assertEquals("포착이", user.getNickname());
  }

  @Test
  void changeNicknameThrowsWhenUserNotFound() {
    UserRepository userRepository = mock(UserRepository.class);
    ChangeNicknameService changeNicknameService = new ChangeNicknameService(userRepository);
    given(userRepository.findById(1L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> changeNicknameService.changeNickname(1L, "포착이"));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }
}
