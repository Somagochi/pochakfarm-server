package com.somagochi.pochakfarm.characterization.infrastructure.persistence;

import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterizationRepository extends JpaRepository<Characterization, Long> {

  boolean existsByDeviceIdAndStatus(Long deviceId, CharacterizationStatus status);
}
