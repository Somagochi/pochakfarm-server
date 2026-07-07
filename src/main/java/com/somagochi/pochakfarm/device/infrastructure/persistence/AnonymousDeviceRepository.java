package com.somagochi.pochakfarm.device.infrastructure.persistence;

import com.somagochi.pochakfarm.device.domain.AnonymousDevice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnonymousDeviceRepository extends JpaRepository<AnonymousDevice, Long> {

  Optional<AnonymousDevice> findByDeviceToken(String deviceToken);
}
