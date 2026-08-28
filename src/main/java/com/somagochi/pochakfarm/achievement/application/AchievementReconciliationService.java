package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.dto.AchievementReconciliationResult;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementReconciliationService {

  static final int MAX_USER_COUNT = 1_000;
  private static final int CHUNK_SIZE = 100;

  private final AchievementEvaluator achievementEvaluator;

  public AchievementReconciliationResult reconcile(List<Long> userIds) {
    validate(userIds);
    List<Long> distinctUserIds = new ArrayList<>(new LinkedHashSet<>(userIds));
    List<Long> failedUserIds = new ArrayList<>();

    for (int start = 0; start < distinctUserIds.size(); start += CHUNK_SIZE) {
      int end = Math.min(start + CHUNK_SIZE, distinctUserIds.size());
      reconcileSequentially(distinctUserIds.subList(start, end), failedUserIds);
    }

    return new AchievementReconciliationResult(
        userIds.size(),
        distinctUserIds.size(),
        distinctUserIds.size() - failedUserIds.size(),
        failedUserIds);
  }

  private void reconcileSequentially(List<Long> userIds, List<Long> failedUserIds) {
    for (Long userId : userIds) {
      try {
        achievementEvaluator.reconcile(userId);
      } catch (RuntimeException exception) {
        failedUserIds.add(userId);
        log.error("사용자 업적 대사 실패 userId={}", userId, exception);
      }
    }
  }

  private void validate(List<Long> userIds) {
    if (userIds == null
        || userIds.isEmpty()
        || userIds.size() > MAX_USER_COUNT
        || userIds.stream().anyMatch(userId -> userId == null || userId <= 0)) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
  }
}
