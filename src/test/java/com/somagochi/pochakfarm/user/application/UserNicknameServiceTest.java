package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

class UserNicknameServiceTest {

  @Test
  void changeNicknameUpdatesNickname() {
    UserRepository userRepository = mock(UserRepository.class);
    UserNicknameService userNicknameService = new UserNicknameService(userRepository);
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.empty());
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));

    NicknameResponse response = userNicknameService.changeNickname(1L, "포착이");

    assertEquals("포착이", response.nickname());
    assertEquals("포착이", user.getNickname());
  }

  @Test
  void changeNicknameThrowsWhenNicknameAlreadyUsed() {
    UserRepository userRepository = mock(UserRepository.class);
    UserNicknameService userNicknameService = new UserNicknameService(userRepository);
    User other = User.register(SocialProvider.NAVER, "provider-id-2", "other@test.com");
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.of(other));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> userNicknameService.changeNickname(1L, "포착이"));

    assertEquals(ErrorCode.DUPLICATE_NICKNAME.getCode(), exception.getCode());
  }

  @Test
  void changeNicknameThrowsWhenUserNotFound() {
    UserRepository userRepository = mock(UserRepository.class);
    UserNicknameService userNicknameService = new UserNicknameService(userRepository);
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.empty());
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> userNicknameService.changeNickname(1L, "포착이"));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void checkNicknamePassesWhenNicknameIsAvailable() {
    UserRepository userRepository = mock(UserRepository.class);
    UserNicknameService userNicknameService = new UserNicknameService(userRepository);
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.empty());

    assertDoesNotThrow(() -> userNicknameService.checkNickname("포착이"));
  }

  @Test
  void checkNicknameThrowsWhenNicknameAlreadyUsed() {
    UserRepository userRepository = mock(UserRepository.class);
    UserNicknameService userNicknameService = new UserNicknameService(userRepository);
    User other = User.register(SocialProvider.NAVER, "provider-id-2", "other@test.com");
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.of(other));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> userNicknameService.checkNickname("포착이"));

    assertEquals(ErrorCode.DUPLICATE_NICKNAME.getCode(), exception.getCode());
  }
}
