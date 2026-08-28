package com.somagochi.pochakfarm.achievement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.achievement.dto.AchievementReconciliationResult;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AchievementReconciliationServiceTest {

  private final AchievementEvaluator achievementEvaluator = mock(AchievementEvaluator.class);
  private final AchievementReconciliationService reconciliationService =
      new AchievementReconciliationService(achievementEvaluator);

  @Test
  void deduplicatesUsersAndContinuesAfterIndividualFailure() {
    doThrow(new IllegalStateException("failed")).when(achievementEvaluator).reconcile(2L);

    AchievementReconciliationResult result =
        reconciliationService.reconcile(List.of(1L, 2L, 1L, 3L));

    assertEquals(4, result.requestedCount());
    assertEquals(3, result.distinctCount());
    assertEquals(2, result.succeededCount());
    assertEquals(List.of(2L), result.failedUserIds());
    InOrder order = inOrder(achievementEvaluator);
    order.verify(achievementEvaluator).reconcile(1L);
    order.verify(achievementEvaluator).reconcile(2L);
    order.verify(achievementEvaluator).reconcile(3L);
  }

  @Test
  void rejectsInvalidOrExcessiveUserIds() {
    assertThrows(BusinessException.class, () -> reconciliationService.reconcile(List.of()));
    assertThrows(BusinessException.class, () -> reconciliationService.reconcile(List.of(0L)));

    List<Long> tooMany = new ArrayList<>();
    for (long userId = 1;
        userId <= AchievementReconciliationService.MAX_USER_COUNT + 1L;
        userId++) {
      tooMany.add(userId);
    }
    assertThrows(BusinessException.class, () -> reconciliationService.reconcile(tooMany));
  }
}
