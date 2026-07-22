package com.somagochi.pochakfarm.animal.infrastructure.persistence;

import com.somagochi.pochakfarm.animal.domain.Animal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalRepository extends JpaRepository<Animal, Long> {}
