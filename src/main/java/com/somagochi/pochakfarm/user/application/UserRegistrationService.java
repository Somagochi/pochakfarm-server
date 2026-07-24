package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.farm.application.FarmInitializationService;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.UserRegistration;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

  private final UserRepository userRepository;
  private final FarmInitializationService farmInitializationService;

  public UserRegistrationService(
      UserRepository userRepository, FarmInitializationService farmInitializationService) {
    this.userRepository = userRepository;
    this.farmInitializationService = farmInitializationService;
  }

  @Transactional
  public UserRegistration getOrRegister(SocialUserInfo userInfo) {
    return findBySocialAccount(userInfo)
        .map(user -> new UserRegistration(user, false))
        .orElseGet(() -> new UserRegistration(register(userInfo), true));
  }

  private Optional<User> findBySocialAccount(SocialUserInfo userInfo) {
    return userRepository.findBySocialAccountProviderAndSocialAccountProviderId(
        userInfo.provider(), userInfo.providerId());
  }

  private User register(SocialUserInfo userInfo) {
    try {
      User user =
          userRepository.save(
              User.register(userInfo.provider(), userInfo.providerId(), userInfo.email()));
      farmInitializationService.initialize(user.getId());
      return user;
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.USER_ALREADY_REGISTERED);
    }
  }
}
