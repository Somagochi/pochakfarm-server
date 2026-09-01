package com.somagochi.pochakfarm.animal.domain;

import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AnimalRestColumnTest {

  @Autowired private EntityManager entityManager;

  @Test
  void newAnimalHasNoRestEndsAt() {
    Animal animal = Animal.create(1L, 2L, 1, 1);
    entityManager.persist(animal);
    entityManager.flush();

    Object restEndsAt =
        entityManager
            .createNativeQuery("SELECT rest_ends_at FROM animals WHERE id = :id")
            .setParameter("id", animal.getId())
            .getSingleResult();

    assertNull(restEndsAt);
    assertNull(animal.getRestEndsAt());
  }
}
