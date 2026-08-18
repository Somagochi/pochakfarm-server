package com.somagochi.pochakfarm.animal.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AnimalRepositorySearchTest {

  private static final Long SPACE_ID = 1L;
  private static final Limit LIMIT = Limit.of(13);

  @Autowired private AnimalRepository animalRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void findsOnlyOwnAnimalsWhoseNameStartsWithKeyword() {
    User owner = persistUser("owner");
    User another = persistUser("another");
    Animal first = persistAnimal(owner.getId(), "솜구름");
    Animal second = persistAnimal(owner.getId(), "솜사탕");
    persistAnimal(owner.getId(), "구름솜");
    persistAnimal(another.getId(), "솜별");
    entityManager.flush();

    List<Animal> found =
        animalRepository.searchOwnedAnimalsByName(
            owner.getId(), List.of(CardType.SEA), "솜%", Long.MAX_VALUE, LIMIT);

    assertEquals(
        List.of(second.getId(), first.getId()), found.stream().map(Animal::getId).toList());
  }

  @Test
  void excludesAnimalsOfOtherCardTypes() {
    User owner = persistUser("cardtype");
    Animal sea = persistAnimal(owner.getId(), CardType.SEA, "솜구름");
    persistAnimal(owner.getId(), CardType.SKY, "솜사탕");
    entityManager.flush();

    List<Animal> found =
        animalRepository.searchOwnedAnimalsByName(
            owner.getId(), List.of(CardType.SEA), "솜%", Long.MAX_VALUE, LIMIT);

    assertEquals(List.of(sea.getId()), found.stream().map(Animal::getId).toList());
  }

  @Test
  void searchesEveryCardTypeWhenAllTypesAreGiven() {
    User owner = persistUser("alltype");
    Animal sea = persistAnimal(owner.getId(), CardType.SEA, "솜구름");
    Animal sky = persistAnimal(owner.getId(), CardType.SKY, "솜사탕");
    entityManager.flush();

    List<Animal> found =
        animalRepository.searchOwnedAnimalsByName(
            owner.getId(), List.of(CardType.values()), "솜%", Long.MAX_VALUE, LIMIT);

    assertEquals(List.of(sky.getId(), sea.getId()), found.stream().map(Animal::getId).toList());
  }

  @Test
  void treatsEscapedWildcardInKeywordAsLiteral() {
    User owner = persistUser("wildcard");
    Animal literal = persistAnimal(owner.getId(), "솜_이");
    persistAnimal(owner.getId(), "솜구름");
    entityManager.flush();

    List<Animal> found =
        animalRepository.searchOwnedAnimalsByName(
            owner.getId(), List.of(CardType.SEA), "솜!_%", Long.MAX_VALUE, LIMIT);

    assertEquals(List.of(literal.getId()), found.stream().map(Animal::getId).toList());
  }

  @Test
  void excludesAnimalsBeyondCursor() {
    User owner = persistUser("cursor");
    Animal first = persistAnimal(owner.getId(), "솜구름");
    Animal second = persistAnimal(owner.getId(), "솜사탕");
    entityManager.flush();

    List<Animal> found =
        animalRepository.searchOwnedAnimalsByName(
            owner.getId(), List.of(CardType.SEA), "솜%", second.getId(), LIMIT);

    assertEquals(List.of(first.getId()), found.stream().map(Animal::getId).toList());
  }

  @Test
  void returnsEmptyWhenNoNameMatches() {
    User owner = persistUser("empty");
    persistAnimal(owner.getId(), "솜구름");
    entityManager.flush();

    assertTrue(
        animalRepository
            .searchOwnedAnimalsByName(
                owner.getId(), List.of(CardType.SEA), "바다%", Long.MAX_VALUE, LIMIT)
            .isEmpty());
  }

  private User persistUser(String suffix) {
    User user =
        User.register(
            SocialProvider.KAKAO,
            "search-" + suffix + "-" + UUID.randomUUID(),
            suffix + "@example.com");
    entityManager.persist(user);
    return user;
  }

  private Animal persistAnimal(Long userId, String animalName) {
    return persistAnimal(userId, CardType.SEA, animalName);
  }

  private Animal persistAnimal(Long userId, CardType cardType, String animalName) {
    Capture capture =
        Capture.create(
            userId,
            UUID.randomUUID().toString(),
            cardType,
            Tier.C,
            AnimalName.from(animalName),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            UUID.randomUUID().toString(),
            "images/original.jpg",
            "image/jpeg",
            Instant.parse("2026-08-04T01:00:00Z"));
    entityManager.persist(capture);
    Animal animal = Animal.create(capture.getId(), SPACE_ID, 0, 0);
    entityManager.persist(animal);
    return animal;
  }
}
