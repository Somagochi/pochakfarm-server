package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.NicknameResponse;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserNicknameService {

  private final UserRepository userRepository;

  public UserNicknameService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public NicknameResponse changeNickname(Long userId, String nickname) {
    checkNickname(nickname);

    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.changeNickname(nickname);
    return NicknameResponse.from(user);
  }

  @Transactional(readOnly = true)
  public void checkNickname(String nickname) {
    userRepository
        .findUserByNickname(nickname)
        .ifPresent(
            found -> {
              throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            });
  }
}
