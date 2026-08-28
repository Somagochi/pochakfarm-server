package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementSource;
import com.somagochi.pochakfarm.common.entity.EntityChangedEvent;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Component
public class AchievementSourceResolverRegistry implements SmartInitializingSingleton {

  private final List<AchievementSourceResolver> resolvers;
  private final EntityManagerFactory entityManagerFactory;

  public AchievementSourceResolverRegistry(
      List<AchievementSourceResolver> resolvers, EntityManagerFactory entityManagerFactory) {
    this.resolvers = List.copyOf(resolvers);
    this.entityManagerFactory = entityManagerFactory;
  }

  public List<AchievementEvaluationRequest> resolve(EntityChangedEvent event) {
    List<AchievementSourceResolver> matching =
        resolvers.stream()
            .filter(
                resolver ->
                    resolver.sourceTypes().stream()
                        .anyMatch(type -> type.isInstance(event.entity())))
            .toList();
    if (matching.isEmpty()) {
      throw new IllegalStateException(
          "AchievementSource resolver is missing: " + event.entity().getClass().getName());
    }
    return matching.stream()
        .map(resolver -> resolver.resolve(event))
        .flatMap(Optional::stream)
        .toList();
  }

  @Override
  public void afterSingletonsInstantiated() {
    Set<Class<?>> sourceTypes = new HashSet<>();
    for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
      if (AchievementSource.class.isAssignableFrom(entity.getJavaType())) {
        sourceTypes.add(entity.getJavaType());
      }
    }
    Set<Class<?>> resolvedTypes = new HashSet<>();
    resolvers.forEach(resolver -> resolvedTypes.addAll(resolver.sourceTypes()));
    sourceTypes.removeAll(resolvedTypes);
    if (!sourceTypes.isEmpty()) {
      throw new IllegalStateException("AchievementSource resolver is missing: " + sourceTypes);
    }
  }
}
