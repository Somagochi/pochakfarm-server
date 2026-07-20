package com.somagochi.pochakfarm.characterization.application;

import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterizationStatusService {

  private final CharacterizationRepository characterizationRepository;

  public CharacterizationStatusService(CharacterizationRepository characterizationRepository) {
    this.characterizationRepository = characterizationRepository;
  }

  @Transactional
  public void succeed(
      Long characterizationId, String resultImageKey, String provider, Integer elapsedMs) {
    Characterization characterization = find(characterizationId);
    characterization.succeed(resultImageKey, provider, elapsedMs);
  }

  @Transactional
  public void fail(Long characterizationId, String failureReason) {
    Characterization characterization = find(characterizationId);
    characterization.fail(failureReason);
  }

  private Characterization find(Long characterizationId) {
    return characterizationRepository
        .findById(characterizationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTERIZATION_NOT_FOUND));
  }
}
