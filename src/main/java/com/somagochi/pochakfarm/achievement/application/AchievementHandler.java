package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementSource;
import com.somagochi.pochakfarm.common.entity.EntityChangedEvent;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementHandler {

  private final AchievementSourceResolverRegistry sourceResolverRegistry;
  private final AchievementEvaluator achievementEvaluator;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handle(EntityChangedEvent event) {
    // 트랜잭션 안에서 발행된 이벤트는 commit 뒤 처리하고, 추후 서비스 계층에서 직접 발행하는
    // 비트랜잭션 이벤트는 즉시 처리한다. 실제 업적 판정·저장은 별도 REQUIRES_NEW 트랜잭션이다.
    // TODO: 동시 변경 사이의 일시적 달성까지 보장하려면 원본 트랜잭션의 상태 스냅샷이나
    // Outbox가 필요하다. 현재는 커밋 직후 DB의 최신 상태를 기준으로 판정한다.
    if (!(event.entity() instanceof AchievementSource)) {
      return;
    }
    Map<Long, EnumSet<AchievementMetric>> metricsByUser = new LinkedHashMap<>();
    try {
      for (AchievementEvaluationRequest request : sourceResolverRegistry.resolve(event)) {
        metricsByUser
            .computeIfAbsent(request.userId(), ignored -> EnumSet.noneOf(AchievementMetric.class))
            .addAll(request.metrics());
      }
    } catch (RuntimeException exception) {
      log.error(
          "엔티티 변경의 업적 평가 요청 해석 실패 entity={} changeType={}",
          event.entity().getClass().getSimpleName(),
          event.changeType(),
          exception);
      return;
    }
    metricsByUser.forEach((userId, metrics) -> evaluateSafely(userId, metrics, event));
  }

  private void evaluateSafely(
      Long userId, EnumSet<AchievementMetric> metrics, EntityChangedEvent event) {
    try {
      achievementEvaluator.evaluate(userId, metrics);
    } catch (RuntimeException exception) {
      log.error(
          "엔티티 변경 기반 업적 판정 실패 userId={} entity={} changeType={}",
          userId,
          event.entity().getClass().getSimpleName(),
          event.changeType(),
          exception);
    }
  }
}
