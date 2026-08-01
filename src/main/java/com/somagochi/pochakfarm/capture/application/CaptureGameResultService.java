package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.CaptureExperiencePolicy;
import com.somagochi.pochakfarm.capture.domain.CaptureGameResultPolicy;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse.ProgressionState;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.domain.LevelReward;
import com.somagochi.pochakfarm.user.domain.LevelRewardPolicy;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaptureGameResultService {

  private final CaptureRepository captureRepository;
  private final UserRepository userRepository;
  private final CaptureGameResultPolicy gameResultPolicy;
  private final CaptureExperiencePolicy experiencePolicy;
  private final LevelRewardPolicy levelRewardPolicy;
  private final Clock clock;

  @Transactional
  public CaptureGameResultResponse submit(
      Long userId, Long captureId, CaptureGameResultRequest request) {
    Instant receivedAt = clock.instant();
    Capture capture =
        captureRepository
            .findByIdForUpdate(captureId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CAPTURE_NOT_FOUND));
    if (!capture.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_CAPTURE_ACCESS);
    }
    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    if (!capture.isGamePending()) {
      return response(capture, null, progressionState(user));
    }
    if (capture.isGameResultExpired(receivedAt)) {
      capture.expireGame();
      return response(capture, null, progressionState(user));
    }

    GameStatus result = gameResultPolicy.resolve(request == null ? null : request.toDomain());
    long experience = experiencePolicy.experienceFor(capture.getTier(), result);
    ProgressionState before = progressionState(user);
    LevelReward levelReward =
        levelRewardPolicy.calculate(user.getLevel(), user.getExperience(), experience);
    user.applyLevelReward(levelReward);
    capture.completeGame(result, levelReward.experienceReward());
    return response(capture, before, progressionState(user));
  }

  private CaptureGameResultResponse response(
      Capture capture, ProgressionState before, ProgressionState after) {
    return CaptureGameResultResponse.from(capture, before, after);
  }

  private ProgressionState progressionState(User user) {
    return new ProgressionState(
        user.getLevel(),
        user.getExperience(),
        levelRewardPolicy.requiredExperienceForNextLevel(user.getLevel()));
  }
}
