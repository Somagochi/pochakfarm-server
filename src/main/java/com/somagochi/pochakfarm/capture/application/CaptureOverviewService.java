package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.TierSelectionPolicy;
import com.somagochi.pochakfarm.capture.dto.CaptureOverviewResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureOverviewResponse.CaptureCount;
import com.somagochi.pochakfarm.capture.dto.CaptureOverviewResponse.Level;
import com.somagochi.pochakfarm.capture.dto.CaptureTypeCount;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.domain.LevelRewardPolicy;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaptureOverviewService {

  private static final List<CardType> CAPTURE_COUNT_ORDER =
      List.of(CardType.SKY, CardType.GROUND, CardType.SEA, CardType.SPACE);

  private final CaptureRepository captureRepository;
  private final UserRepository userRepository;
  private final LevelRewardPolicy levelRewardPolicy;
  private final TierSelectionPolicy tierSelectionPolicy;

  @Transactional(readOnly = true)
  public CaptureOverviewResponse getOverview(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    long requiredExperience = levelRewardPolicy.requiredExperienceForNextLevel(user.getLevel());
    Level level =
        new Level(
            user.getLevel(),
            user.getExperience(),
            requiredExperience,
            requiredExperience - user.getExperience());
    List<CaptureCount> captureCounts = captureCounts(userId);
    return new CaptureOverviewResponse(
        level, captureCounts, tierSelectionPolicy.probabilitiesFor(user.getLevel()));
  }

  private List<CaptureCount> captureCounts(Long userId) {
    Map<CardType, Long> countsByType = new EnumMap<>(CardType.class);
    for (CaptureTypeCount count : captureRepository.countSucceededByCardType(userId)) {
      countsByType.put(count.cardType(), count.count());
    }
    return CAPTURE_COUNT_ORDER.stream()
        .map(cardType -> new CaptureCount(cardType, countsByType.getOrDefault(cardType, 0L)))
        .toList();
  }
}
