package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.farm.application.FarmInitializationService;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.UserRegistration;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

  private final UserRepository userRepository;
  private final FarmInitializationService farmInitializationService;
  private final UserNicknameService userNicknameService;

  public UserRegistrationService(
      UserRepository userRepository,
      FarmInitializationService farmInitializationService,
      UserNicknameService userNicknameService) {
    this.userRepository = userRepository;
    this.farmInitializationService = farmInitializationService;
    this.userNicknameService = userNicknameService;
  }

  @Transactional
  public UserRegistration getOrRegister(SocialUserInfo userInfo) {
    return findBySocialAccount(userInfo)
        .map(user -> new UserRegistration(user, false))
        .orElseGet(() -> new UserRegistration(register(userInfo), true));
  }

  private Optional<User> findBySocialAccount(SocialUserInfo userInfo) {
    return userRepository.findBySocialAccountProviderAndEmail(
        userInfo.provider(), userInfo.email());
  }

  private User register(SocialUserInfo userInfo) {
    String nickname = userNicknameService.generateUnique();
    User user =
        userRepository.save(
            User.register(userInfo.provider(), userInfo.providerId(), userInfo.email(), nickname));
    farmInitializationService.initialize(user.getId());
    return user;
  }
}
