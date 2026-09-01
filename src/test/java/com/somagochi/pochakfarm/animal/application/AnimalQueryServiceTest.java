package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalDetailResponse;
import com.somagochi.pochakfarm.animal.dto.AnimalPosition;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Limit;

class AnimalQueryServiceTest {

  private final AnimalRepository animalRepository = mock(AnimalRepository.class);
  private final CaptureRepository captureRepository = mock(CaptureRepository.class);
  private final FileStorage fileStorage = mock(FileStorage.class);
  private final AnimalQueryService service =
      new AnimalQueryService(animalRepository, captureRepository, fileStorage);

  @Test
  void getMyAnimalsReturnsFirstPageWithNextCursorWhenMoreExist() {
    List<Animal> animals = new ArrayList<>();
    List<Capture> captures = new ArrayList<>();
    for (int i = 0; i < 13; i++) {
      long id = 100L - i;
      animals.add(animal(id, null, null));
      captures.add(capture(id));
    }
    given(animalRepository.findOwnedAnimals(eq(1L), any(), eq(Long.MAX_VALUE), eq(Limit.of(13))))
        .willReturn(animals);
    given(captureRepository.findAllById(any())).willReturn(captures);

    CursorPage<AnimalResponse> page = service.getMyAnimals(1L, null, null);

    assertEquals(12, page.content().size());
    assertEquals(100L, page.content().get(0).animalId());
    assertTrue(page.hasNext());
    assertEquals(89L, page.nextCursor());
  }

  @Test
  void getMyAnimalsHasNoNextOnLastPage() {
    Animal animal = animal(50L, null, null);
    Capture capture = capture(50L);
    given(animalRepository.findOwnedAnimals(eq(1L), any(), eq(Long.MAX_VALUE), eq(Limit.of(13))))
        .willReturn(List.of(animal));
    given(captureRepository.findAllById(any())).willReturn(List.of(capture));
    given(fileStorage.buildUrl("card-50")).willReturn("https://cdn.test/card.png");

    CursorPage<AnimalResponse> page = service.getMyAnimals(1L, null, null);

    assertEquals(1, page.content().size());
    assertEquals(50L, page.content().get(0).animalId());
    assertEquals("https://cdn.test/card.png", page.content().get(0).cardImageUrl());
    assertFalse(page.hasNext());
    assertNull(page.nextCursor());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getMyAnimalsQueriesEveryCardTypeWhenTypeIsNotGiven() {
    given(animalRepository.findOwnedAnimals(any(), any(), any(), any())).willReturn(List.of());

    service.getMyAnimals(1L, null, null);

    ArgumentCaptor<Collection<CardType>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(animalRepository).findOwnedAnimals(eq(1L), captor.capture(), eq(Long.MAX_VALUE), any());
    assertEquals(Set.of(CardType.values()), Set.copyOf(captor.getValue()));
  }

  @Test
  void getMyAnimalsUsesCursorAndCardType() {
    Animal animal = animal(30L, null, null);
    Capture capture = capture(30L);
    given(
            animalRepository.findOwnedAnimals(
                eq(1L), eq(List.of(CardType.SEA)), eq(50L), eq(Limit.of(13))))
        .willReturn(List.of(animal));
    given(captureRepository.findAllById(any())).willReturn(List.of(capture));

    CursorPage<AnimalResponse> page = service.getMyAnimals(1L, CardType.SEA, 50L);

    assertEquals(1, page.content().size());
    assertEquals(30L, page.content().get(0).animalId());
    assertEquals(CardType.SEA, page.content().get(0).cardType());
    assertFalse(page.hasNext());
  }

  @Test
  void getMyAnimalsReturnsEmptyWhenUserHasNoAnimals() {
    given(animalRepository.findOwnedAnimals(any(), any(), any(), any())).willReturn(List.of());

    CursorPage<AnimalResponse> page = service.getMyAnimals(1L, null, null);

    assertTrue(page.content().isEmpty());
    assertFalse(page.hasNext());
    assertNull(page.nextCursor());
    verifyNoInteractions(captureRepository);
  }

  @Test
  void searchMyAnimalsMatchesNamePrefixWithinOwnAnimalsOfCardType() {
    Animal animal = animal(30L, null, null);
    Capture capture = capture(30L);
    given(
            animalRepository.searchOwnedAnimalsByName(
                eq(1L),
                eq(List.of(CardType.SEA)),
                eq("\uc19c%"),
                eq(Long.MAX_VALUE),
                eq(Limit.of(13))))
        .willReturn(List.of(animal));
    given(captureRepository.findAllById(any())).willReturn(List.of(capture));

    CursorPage<AnimalResponse> page = service.searchMyAnimals(1L, CardType.SEA, "\uc19c", null);

    assertEquals(1, page.content().size());
    assertEquals(30L, page.content().get(0).animalId());
    assertFalse(page.hasNext());
    assertNull(page.nextCursor());
  }

  @Test
  @SuppressWarnings("unchecked")
  void searchMyAnimalsQueriesEveryCardTypeWhenTypeIsNotGiven() {
    given(animalRepository.searchOwnedAnimalsByName(any(), any(), any(), any(), any()))
        .willReturn(List.of());

    service.searchMyAnimals(1L, null, "솜", null);

    ArgumentCaptor<Collection<CardType>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(animalRepository)
        .searchOwnedAnimalsByName(
            eq(1L), captor.capture(), eq("솜%"), eq(Long.MAX_VALUE), eq(Limit.of(13)));
    assertEquals(Set.of(CardType.values()), Set.copyOf(captor.getValue()));
  }

  @Test
  void searchMyAnimalsTrimsKeywordAndEscapesLikeWildcards() {
    given(animalRepository.searchOwnedAnimalsByName(any(), any(), any(), any(), any()))
        .willReturn(List.of());

    service.searchMyAnimals(1L, CardType.SEA, "  1_0%! ", null);

    verify(animalRepository)
        .searchOwnedAnimalsByName(
            eq(1L),
            eq(List.of(CardType.SEA)),
            eq("1!_0!%!!%"),
            eq(Long.MAX_VALUE),
            eq(Limit.of(13)));
  }

  @Test
  void searchMyAnimalsUsesCursorAndReturnsNextCursorWhenMoreExist() {
    List<Animal> animals = new ArrayList<>();
    List<Capture> captures = new ArrayList<>();
    for (int i = 0; i < 13; i++) {
      long id = 40L - i;
      animals.add(animal(id, null, null));
      captures.add(capture(id));
    }
    given(
            animalRepository.searchOwnedAnimalsByName(
                eq(1L), eq(List.of(CardType.SEA)), eq("\ub3d9%"), eq(50L), eq(Limit.of(13))))
        .willReturn(animals);
    given(captureRepository.findAllById(any())).willReturn(captures);

    CursorPage<AnimalResponse> page = service.searchMyAnimals(1L, CardType.SEA, "\ub3d9", 50L);

    assertEquals(12, page.content().size());
    assertTrue(page.hasNext());
    assertEquals(29L, page.nextCursor());
  }

  @Test
  void searchMyAnimalsReturnsEmptyWhenNothingMatches() {
    given(animalRepository.searchOwnedAnimalsByName(any(), any(), any(), any(), any()))
        .willReturn(List.of());

    CursorPage<AnimalResponse> page =
        service.searchMyAnimals(1L, CardType.SEA, "\uc5c6\ub294\uc774\ub984", null);

    assertTrue(page.content().isEmpty());
    assertFalse(page.hasNext());
    assertNull(page.nextCursor());
    verifyNoInteractions(captureRepository);
  }

  @Test
  void searchMyAnimalsThrowsWhenKeywordIsMissingOrBlank() {
    assertThrows(
        BusinessException.class, () -> service.searchMyAnimals(1L, CardType.SEA, null, null));
    assertThrows(
        BusinessException.class, () -> service.searchMyAnimals(1L, CardType.SEA, "   ", null));

    verifyNoInteractions(animalRepository, captureRepository);
  }

  @Test
  void getByFloorRangeReturnsEmptyWithoutLoadingCapturesWhenNoAnimals() {
    given(animalRepository.findBySpaceIdAndFloorNumBetween(100L, 1, 4)).willReturn(List.of());

    assertTrue(service.getByFloorRange(100L, 1, 4).isEmpty());
    verifyNoInteractions(captureRepository, fileStorage);
  }

  @Test
  void getByFloorRangeMapsPositionToAnimal() {
    Animal animal = animal(7L, 2, 3);
    Capture capture = capture(7L);
    given(animalRepository.findBySpaceIdAndFloorNumBetween(100L, 1, 4)).willReturn(List.of(animal));
    given(captureRepository.findAllById(any())).willReturn(List.of(capture));

    Map<AnimalPosition, AnimalResponse> byPosition = service.getByFloorRange(100L, 1, 4);

    assertEquals(1, byPosition.size());
    assertEquals(7L, byPosition.get(new AnimalPosition(2, 3)).animalId());
  }

  @Test
  void getAnimalReturnsDetailWithMappedSkills() {
    Capture capture = mock(Capture.class);
    given(capture.getAnimalName()).willReturn("바다냥");
    given(capture.getCardType()).willReturn(CardType.SEA);
    given(capture.getTier()).willReturn(Tier.A);
    given(capture.getSkill1()).willReturn(CardSkill.SEA_WAVE_DASH);
    given(capture.getSkill2()).willReturn(CardSkill.SEA_BUBBLE_GUARD);
    given(capture.getCardImage()).willReturn("card-key");
    given(capture.getAnimalImage()).willReturn("animal-key");
    given(fileStorage.buildUrl("card-key")).willReturn("https://cdn/card");
    given(fileStorage.buildUrl("animal-key")).willReturn("https://cdn/animal");
    given(captureRepository.findByUserIdAndAnimalId(1L, 10L)).willReturn(Optional.of(capture));

    AnimalDetailResponse response = service.getAnimal(1L, 10L);

    assertEquals(10L, response.animalId());
    assertEquals("바다냥", response.animalName());
    assertEquals(CardType.SEA, response.cardType());
    assertEquals(Tier.A, response.tier());
    assertEquals(CardSkill.SEA_WAVE_DASH.displayName(), response.skill1().name());
    assertEquals(CardSkill.SEA_WAVE_DASH.description(), response.skill1().description());
    assertEquals(CardSkill.SEA_BUBBLE_GUARD.displayName(), response.skill2().name());
    assertEquals(CardSkill.SEA_BUBBLE_GUARD.description(), response.skill2().description());
    assertEquals("https://cdn/card", response.cardImageUrl());
    assertEquals("https://cdn/animal", response.animalImageUrl());
  }

  @Test
  void getAnimalKeepsNullSkillAsNull() {
    Capture capture = mock(Capture.class);
    given(capture.getCardType()).willReturn(CardType.SEA);
    given(capture.getTier()).willReturn(Tier.A);
    given(capture.getSkill1()).willReturn(null);
    given(capture.getSkill2()).willReturn(null);
    given(captureRepository.findByUserIdAndAnimalId(1L, 10L)).willReturn(Optional.of(capture));

    AnimalDetailResponse response = service.getAnimal(1L, 10L);

    assertNull(response.skill1());
    assertNull(response.skill2());
  }

  @Test
  void getAnimalThrowsWhenCaptureNotFound() {
    given(captureRepository.findByUserIdAndAnimalId(1L, 10L)).willReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> service.getAnimal(1L, 10L));
  }

  private Animal animal(long id, Integer floorNum, Integer slotNum) {
    Animal animal = mock(Animal.class);
    given(animal.getId()).willReturn(id);
    given(animal.getCaptureId()).willReturn(id);
    given(animal.getFloorNum()).willReturn(floorNum);
    given(animal.getSlotNum()).willReturn(slotNum);
    return animal;
  }

  private Capture capture(long id) {
    Capture capture = mock(Capture.class);
    given(capture.getId()).willReturn(id);
    given(capture.getAnimalName()).willReturn("동물" + id);
    given(capture.getCardType()).willReturn(CardType.SEA);
    given(capture.getTier()).willReturn(Tier.A);
    given(capture.getCardImage()).willReturn("card-" + id);
    given(capture.getAnimalImage()).willReturn("animal-" + id);
    return capture;
  }
}
