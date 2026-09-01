package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.farm.application.FarmInitializationService;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.UserRegistration;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class UserRegistrationServiceTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final FarmInitializationService farmInitializationService =
      mock(FarmInitializationService.class);
  private final UserNicknameService userNicknameService = mock(UserNicknameService.class);
  private final UserRegistrationService userRegistrationService =
      new UserRegistrationService(userRepository, farmInitializationService, userNicknameService);

  @Test
  void returnsExistingUserWhenSocialAccountAlreadyRegistered() {
    SocialUserInfo userInfo =
        new SocialUserInfo(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    User user = User.register(SocialProvider.KAKAO, "kakao-123", "user@kakao.com", "행복토끼07");
    given(
            userRepository.findBySocialAccountProviderAndEmail(
                SocialProvider.KAKAO, "user@kakao.com"))
        .willReturn(Optional.of(user));

    UserRegistration registration = userRegistrationService.getOrRegister(userInfo);

    assertEquals(user, registration.user());
    assertFalse(registration.isNew());
  }

  @Test
  void registersNewUserWithGeneratedNickname() {
    SocialUserInfo userInfo =
        new SocialUserInfo(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    User user = User.register(SocialProvider.KAKAO, "kakao-123", "user@kakao.com", "행복토끼07");
    given(
            userRepository.findBySocialAccountProviderAndEmail(
                SocialProvider.KAKAO, "user@kakao.com"))
        .willReturn(Optional.empty());
    given(userNicknameService.generateUnique()).willReturn("행복토끼07");
    given(userRepository.save(any(User.class))).willReturn(user);

    UserRegistration registration = userRegistrationService.getOrRegister(userInfo);

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    assertEquals("행복토끼07", saved.getValue().getNickname());
    assertEquals(user, registration.user());
    assertTrue(registration.isNew());
    verify(farmInitializationService).initialize(user.getId());
  }

  @Test
  void propagatesUniqueViolationWhenConcurrentRegistrationCollides() {
    SocialUserInfo userInfo =
        new SocialUserInfo(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    given(
            userRepository.findBySocialAccountProviderAndEmail(
                SocialProvider.KAKAO, "user@kakao.com"))
        .willReturn(Optional.empty());
    given(userNicknameService.generateUnique()).willReturn("행복토끼07");
    given(userRepository.save(any(User.class)))
        .willThrow(new DataIntegrityViolationException("duplicate"));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> userRegistrationService.getOrRegister(userInfo));
  }
}
