package com.somagochi.pochakfarm.user.infrastructure.persistence;

import com.somagochi.pochakfarm.user.domain.CoinHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoinHistoryRepository extends JpaRepository<CoinHistory, Long> {}
