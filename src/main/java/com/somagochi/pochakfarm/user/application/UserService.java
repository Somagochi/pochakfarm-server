package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.UserRegistration;
import com.somagochi.pochakfarm.user.dto.UserResponse;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public UserResponse getProfile(Long userId) {
    return UserResponse.from(
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND)));
  }

  @Transactional
  public void withdraw(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.withdraw();
  }

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
      return userRepository.save(
          User.register(userInfo.provider(), userInfo.providerId(), userInfo.email()));
    } catch (DataIntegrityViolationException exception) {
      return findBySocialAccount(userInfo).orElseThrow(() -> exception);
    }
  }
}
