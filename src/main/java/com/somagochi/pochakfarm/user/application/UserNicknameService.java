package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.domain.NicknameGenerator;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.NicknameResponse;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserNicknameService {

  private static final int MAX_GENERATION_ATTEMPTS = 10;

  private final UserRepository userRepository;
  private final NicknameGenerator nicknameGenerator;

  public UserNicknameService(UserRepository userRepository, NicknameGenerator nicknameGenerator) {
    this.userRepository = userRepository;
    this.nicknameGenerator = nicknameGenerator;
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

  public String generateUnique() {
    for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
      String candidate = nicknameGenerator.generate();
      if (userRepository.findUserByNickname(candidate).isEmpty()) {
        return candidate;
      }
    }
    log.warn("사용 가능한 닉네임을 {}회 안에 찾지 못했습니다", MAX_GENERATION_ATTEMPTS);
    return nicknameGenerator.generate();
  }
}
