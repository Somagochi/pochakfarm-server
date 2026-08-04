package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.domain.LevelRewardPolicy;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.UserProfileResponse;
import com.somagochi.pochakfarm.user.dto.UserResponse;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserQueryService {

  private final UserRepository userRepository;
  private final LevelRewardPolicy levelRewardPolicy;

  public UserQueryService(UserRepository userRepository, LevelRewardPolicy levelRewardPolicy) {
    this.userRepository = userRepository;
    this.levelRewardPolicy = levelRewardPolicy;
  }

  @Transactional(readOnly = true)
  public UserResponse getMe(Long userId) {
    return UserResponse.from(findUser(userId));
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(Long userId) {
    User user = findUser(userId);
    long requiredExperience = levelRewardPolicy.requiredExperienceForNextLevel(user.getLevel());
    long remainingExperience = requiredExperience - user.getExperience();
    return UserProfileResponse.from(user, requiredExperience, remainingExperience);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public User getForUpdate(Long userId) {
    return userRepository
        .findByIdForUpdate(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }

  private User findUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }
}
