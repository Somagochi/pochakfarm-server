package com.somagochi.pochakfarm.common.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.characterization")
public record CharacterizationProperties(boolean deviceLimitEnabled, Reaper reaper) {

  public record Reaper(boolean enabled, Duration interval, Duration staleThreshold) {}
}
