package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.NicknameGenerator;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.NicknameResponse;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserNicknameServiceTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final NicknameGenerator nicknameGenerator = mock(NicknameGenerator.class);
  private final UserNicknameService userNicknameService =
      new UserNicknameService(userRepository, nicknameGenerator);

  @Test
  void changeNicknameUpdatesNickname() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com", "행복토끼07");
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.empty());
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));

    NicknameResponse response = userNicknameService.changeNickname(1L, "포착이");

    assertEquals("포착이", response.nickname());
    assertEquals("포착이", user.getNickname());
  }

  @Test
  void changeNicknameThrowsWhenNicknameAlreadyUsed() {
    User other = User.register(SocialProvider.NAVER, "provider-id-2", "other@test.com", "용감여우42");
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.of(other));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> userNicknameService.changeNickname(1L, "포착이"));

    assertEquals(ErrorCode.DUPLICATE_NICKNAME.getCode(), exception.getCode());
  }

  @Test
  void changeNicknameThrowsWhenUserNotFound() {
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.empty());
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> userNicknameService.changeNickname(1L, "포착이"));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void checkNicknamePassesWhenNicknameIsAvailable() {
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.empty());

    assertDoesNotThrow(() -> userNicknameService.checkNickname("포착이"));
  }

  @Test
  void checkNicknameThrowsWhenNicknameAlreadyUsed() {
    User other = User.register(SocialProvider.NAVER, "provider-id-2", "other@test.com", "용감여우42");
    given(userRepository.findUserByNickname("포착이")).willReturn(Optional.of(other));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> userNicknameService.checkNickname("포착이"));

    assertEquals(ErrorCode.DUPLICATE_NICKNAME.getCode(), exception.getCode());
  }

  @Test
  void generateUniqueReturnsFirstAvailableCandidate() {
    given(nicknameGenerator.generate()).willReturn("행복토끼07");
    given(userRepository.findUserByNickname("행복토끼07")).willReturn(Optional.empty());

    assertEquals("행복토끼07", userNicknameService.generateUnique());
  }

  @Test
  void generateUniqueRetriesUntilCandidateIsAvailable() {
    User taken = User.register(SocialProvider.KAKAO, "provider-id-3", "taken@test.com", "행복토끼07");
    given(nicknameGenerator.generate()).willReturn("행복토끼07", "행복토끼07", "용감여우42");
    given(userRepository.findUserByNickname("행복토끼07")).willReturn(Optional.of(taken));
    given(userRepository.findUserByNickname("용감여우42")).willReturn(Optional.empty());

    assertEquals("용감여우42", userNicknameService.generateUnique());
  }

  @Test
  void generateUniqueFallsBackToLastCandidateWhenAttemptsExhausted() {
    User taken = User.register(SocialProvider.KAKAO, "provider-id-4", "taken@test.com", "행복토끼07");
    given(nicknameGenerator.generate()).willReturn("행복토끼07");
    given(userRepository.findUserByNickname("행복토끼07")).willReturn(Optional.of(taken));

    assertEquals("행복토끼07", userNicknameService.generateUnique());
  }
}
