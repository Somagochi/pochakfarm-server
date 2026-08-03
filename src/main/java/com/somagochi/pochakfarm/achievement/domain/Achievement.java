package com.somagochi.pochakfarm.achievement.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "achievements",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_achievements_code",
          columnNames = {"code"})
    })
@SQLRestriction("deleted_at is null")
public class Achievement extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, updatable = false, length = 64)
  private String code;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "description")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 32)
  private AchievementCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "metric", nullable = false, length = 64)
  private AchievementMetric metric;

  @Column(name = "metric_param", length = 32)
  private String metricParam;

  @Column(name = "target_value", nullable = false)
  private long targetValue;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "hidden", nullable = false)
  private boolean hidden;

  @Column(name = "unachieved_image_key")
  private String unachievedImageKey;

  @Column(name = "achieved_image_key")
  private String achievedImageKey;

  private Achievement(
      String code,
      String title,
      String description,
      AchievementCategory category,
      AchievementMetric metric,
      String metricParam,
      long targetValue,
      boolean enabled,
      boolean hidden,
      String unachievedImageKey,
      String achievedImageKey) {
    this.code = Objects.requireNonNull(code);
    this.title = Objects.requireNonNull(title);
    this.description = description;
    this.category = Objects.requireNonNull(category);
    this.metric = Objects.requireNonNull(metric);
    this.metricParam = metricParam;
    this.targetValue = targetValue;
    this.enabled = enabled;
    this.hidden = hidden;
    this.unachievedImageKey = unachievedImageKey;
    this.achievedImageKey = achievedImageKey;
  }

  public static Achievement create(
      String code,
      String title,
      String description,
      AchievementCategory category,
      AchievementMetric metric,
      String metricParam,
      long targetValue) {
    return create(code, title, description, category, metric, metricParam, targetValue, null, null);
  }

  public static Achievement create(
      String code,
      String title,
      String description,
      AchievementCategory category,
      AchievementMetric metric,
      String metricParam,
      long targetValue,
      String unachievedImageKey,
      String achievedImageKey) {
    return new Achievement(
        code,
        title,
        description,
        category,
        metric,
        metricParam,
        targetValue,
        true,
        false,
        unachievedImageKey,
        achievedImageKey);
  }

  public static Achievement createHidden(
      String code,
      String title,
      String description,
      AchievementCategory category,
      AchievementMetric metric,
      String metricParam,
      long targetValue) {
    return createHidden(
        code, title, description, category, metric, metricParam, targetValue, null, null);
  }

  public static Achievement createHidden(
      String code,
      String title,
      String description,
      AchievementCategory category,
      AchievementMetric metric,
      String metricParam,
      long targetValue,
      String unachievedImageKey,
      String achievedImageKey) {
    return new Achievement(
        code,
        title,
        description,
        category,
        metric,
        metricParam,
        targetValue,
        true,
        true,
        unachievedImageKey,
        achievedImageKey);
  }

  public boolean isDefinitionValid() {
    return targetValue > 0 && metric.supports(metricParam);
  }

  public long progressOf(AchievementStats stats) {
    return metric.extract(stats, metricParam);
  }

  public boolean isSatisfiedBy(AchievementStats stats) {
    return progressOf(stats) >= targetValue;
  }

  public boolean isListedWhen(boolean achieved) {
    return enabled || achieved;
  }
}
