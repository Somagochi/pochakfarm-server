package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.farm.application.FarmInitializationService;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.UserRegistration;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class UserRegistrationServiceTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final FarmInitializationService farmInitializationService =
      mock(FarmInitializationService.class);
  private final UserRegistrationService userRegistrationService =
      new UserRegistrationService(userRepository, farmInitializationService);

  @Test
  void returnsExistingUserWhenSocialAccountAlreadyRegistered() {
    SocialUserInfo userInfo =
        new SocialUserInfo(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    User user = User.register(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    given(
            userRepository.findBySocialAccountProviderAndEmail(
                SocialProvider.KAKAO, "user@kakao.com"))
        .willReturn(Optional.of(user));

    UserRegistration registration = userRegistrationService.getOrRegister(userInfo);

    assertEquals(user, registration.user());
    assertFalse(registration.isNew());
  }

  @Test
  void registersNewUserWhenSocialAccountNotFound() {
    SocialUserInfo userInfo =
        new SocialUserInfo(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    User user = User.register(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    given(
            userRepository.findBySocialAccountProviderAndEmail(
                SocialProvider.KAKAO, "user@kakao.com"))
        .willReturn(Optional.empty());
    given(userRepository.save(any(User.class))).willReturn(user);

    UserRegistration registration = userRegistrationService.getOrRegister(userInfo);

    assertEquals(user, registration.user());
    assertTrue(registration.isNew());
    verify(farmInitializationService).initialize(user.getId());
  }

  @Test
  void throwsConflictWhenConcurrentRegistrationCollides() {
    SocialUserInfo userInfo =
        new SocialUserInfo(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    given(
            userRepository.findBySocialAccountProviderAndEmail(
                SocialProvider.KAKAO, "user@kakao.com"))
        .willReturn(Optional.empty());
    given(userRepository.save(any(User.class)))
        .willThrow(new DataIntegrityViolationException("duplicate"));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> userRegistrationService.getOrRegister(userInfo));

    assertEquals(ErrorCode.USER_ALREADY_REGISTERED.getCode(), exception.getCode());
    assertEquals(409, exception.getStatus());
  }
}
