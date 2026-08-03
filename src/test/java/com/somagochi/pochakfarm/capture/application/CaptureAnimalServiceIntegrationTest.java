package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.farm.application.FarmInitializationService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.storage.infrastructure.InMemoryFileStorage;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(CaptureAnimalServiceIntegrationTest.TestFileStorageConfig.class)
class CaptureAnimalServiceIntegrationTest {

  @TestConfiguration
  static class TestFileStorageConfig {

    @Bean
    @Primary
    FileStorage inMemoryFileStorage() {
      return new InMemoryFileStorage();
    }
  }

  @Autowired private CaptureAnimalService service;
  @Autowired private CaptureRepository captureRepository;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private FarmSpaceRepository farmSpaceRepository;
  @Autowired private FarmInitializationService farmInitializationService;
  @Autowired private UserRepository userRepository;
  @Autowired private FileStorage fileStorage;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Long userId;

  @BeforeEach
  void setUp() {
    cleanUp();
    User user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO,
                "capture-animal-" + UUID.randomUUID(),
                "capture-animal@test.com"));
    userId = user.getId();
    farmInitializationService.initialize(userId);
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @Test
  void storesImageAndAnimalTogetherAtSelectedEmptySlot() {
    Capture capture = persistPlaceableCapture();
    String key = key(capture.getId());
    upload(key, "image/png");

    CaptureAnimalPlacementResponse response =
        service.place(userId, capture.getId(), new CaptureAnimalPlacementRequest(key, 1, 2, null));

    Capture savedCapture = captureRepository.findById(capture.getId()).orElseThrow();
    Animal savedAnimal = animalRepository.findByCaptureId(capture.getId()).orElseThrow();
    assertEquals(key, savedCapture.getAnimalImage());
    assertEquals(savedAnimal.getId(), response.animalId());
    assertEquals(1, savedAnimal.getFloorNum());
    assertEquals(2, savedAnimal.getSlotNum());
  }

  @Test
  void replacesAnimalAndRetainsItsCaptureHistory() {
    Capture oldCapture = persistPlaceableCapture();
    oldCapture.registerAnimalImage(key(oldCapture.getId()));
    captureRepository.save(oldCapture);
    FarmSpace space =
        farmSpaceRepository.findByUserIdAndType(userId, CardType.GROUND).orElseThrow();
    Animal oldAnimal =
        animalRepository.save(Animal.create(oldCapture.getId(), space.getId(), 1, 1));
    Capture newCapture = persistPlaceableCapture();
    String newKey = key(newCapture.getId());
    upload(newKey, "image/png");

    service.place(
        userId,
        newCapture.getId(),
        new CaptureAnimalPlacementRequest(newKey, 1, 1, oldAnimal.getId()));

    assertFalse(animalRepository.findById(oldAnimal.getId()).isPresent());
    assertTrue(captureRepository.findById(oldCapture.getId()).isPresent());
    Animal replacement = animalRepository.findByCaptureId(newCapture.getId()).orElseThrow();
    assertEquals(1, replacement.getFloorNum());
    assertEquals(1, replacement.getSlotNum());
  }

  @Test
  void returnsSameAnimalForIdenticalRetry() {
    Capture capture = persistPlaceableCapture();
    String key = key(capture.getId());
    upload(key, "image/png");
    CaptureAnimalPlacementRequest request = new CaptureAnimalPlacementRequest(key, 1, 2, null);

    CaptureAnimalPlacementResponse first = service.place(userId, capture.getId(), request);
    CaptureAnimalPlacementResponse second = service.place(userId, capture.getId(), request);

    assertEquals(first, second);
    assertEquals(1, animalRepository.count());
  }

  @Test
  void rollsBackWithoutAnimalWhenUploadedObjectIsNotPng() {
    Capture capture = persistPlaceableCapture();
    String key = key(capture.getId());
    upload(key, "image/jpeg");

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.place(
                    userId, capture.getId(), new CaptureAnimalPlacementRequest(key, 1, 2, null)));

    assertEquals(ErrorCode.UNSUPPORTED_CONTENT_TYPE.getCode(), exception.getCode());
    assertFalse(animalRepository.findByCaptureId(capture.getId()).isPresent());
    assertEquals(null, captureRepository.findById(capture.getId()).orElseThrow().getAnimalImage());
  }

  @Test
  void enforcesOneAnimalPerCaptureAtDatabaseLevel() {
    Capture capture = persistPlaceableCapture();
    FarmSpace space =
        farmSpaceRepository.findByUserIdAndType(userId, CardType.GROUND).orElseThrow();
    animalRepository.saveAndFlush(Animal.create(capture.getId(), space.getId(), 1, 1));

    assertThrows(
        RuntimeException.class,
        () -> animalRepository.saveAndFlush(Animal.create(capture.getId(), space.getId(), 1, 2)));
  }

  private Capture persistPlaceableCapture() {
    Capture capture =
        Capture.create(
            userId,
            UUID.randomUUID().toString(),
            CardType.GROUND,
            Tier.B,
            AnimalName.from("두부"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            "123",
            "images/capture-original/%d/%s.jpg".formatted(userId, UUID.randomUUID()),
            "image/jpeg",
            Instant.parse("2026-08-03T01:05:00Z"));
    capture.succeed("public/capture-scene/scene.png", "public/capture-card/card.png", 100);
    capture.completeGame(GameStatus.SUCCEEDED, 10);
    return captureRepository.saveAndFlush(capture);
  }

  private String key(Long captureId) {
    return "public/capture-animal/%d/%d.png".formatted(userId, captureId);
  }

  private void upload(String key, String contentType) {
    ((InMemoryFileStorage) fileStorage).put(key, 1024, contentType);
  }

  private void cleanUp() {
    jdbcTemplate.update("delete from animals");
    jdbcTemplate.update("delete from captures");
    jdbcTemplate.update("delete from farm_spaces");
    jdbcTemplate.update("delete from users");
  }
}
