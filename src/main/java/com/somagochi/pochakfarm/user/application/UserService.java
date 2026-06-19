package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.user.domain.User;
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
  public User getById(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }

  public User getOrRegister(SocialUserInfo userInfo) {
    return findBySocialAccount(userInfo).orElseGet(() -> register(userInfo));
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
