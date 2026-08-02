package com.somagochi.pochakfarm.badge.infrastructure.persistence;

import com.somagochi.pochakfarm.badge.domain.Badge;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

  Optional<Badge> findByCode(String code);

  List<Badge> findByCodeIn(Collection<String> codes);
}
