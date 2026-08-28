package com.somagochi.pochakfarm.achievement.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.animal.domain.Animal;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AchievementSourceResolverRegistryTest {

  @Test
  void failsStartupWhenJpaAchievementSourceHasNoResolver() {
    EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
    Metamodel metamodel = mock(Metamodel.class);
    @SuppressWarnings("unchecked")
    EntityType<Animal> animalEntity = mock(EntityType.class);
    given(entityManagerFactory.getMetamodel()).willReturn(metamodel);
    given(metamodel.getEntities()).willReturn(Set.of(animalEntity));
    given(animalEntity.getJavaType()).willReturn(Animal.class);
    AchievementSourceResolverRegistry registry =
        new AchievementSourceResolverRegistry(List.of(), entityManagerFactory);

    assertThrows(IllegalStateException.class, registry::afterSingletonsInstantiated);
  }
}
