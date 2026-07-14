package com.somagochi.pochakfarm.characterization.application;

import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterizationReadService {

  private final CharacterizationRepository characterizationRepository;
  private final FileStorage fileStorage;

  public CharacterizationReadService(
      CharacterizationRepository characterizationRepository, FileStorage fileStorage) {
    this.characterizationRepository = characterizationRepository;
    this.fileStorage = fileStorage;
  }

  @Transactional(readOnly = true)
  public CharacterizationResponse getCharacterization(Long characterizationId) {
    Characterization characterization =
        characterizationRepository
            .findById(characterizationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTERIZATION_NOT_FOUND));
    String resultImageUrl = buildUrlOrNull(characterization.getResultImageKey());
    return new CharacterizationResponse(
        characterization.getId(),
        characterization.getStatus(),
        resultImageUrl,
        characterization.getFailureReason());
  }

  private String buildUrlOrNull(String key) {
    return key == null ? null : fileStorage.buildUrl(key);
  }
}
