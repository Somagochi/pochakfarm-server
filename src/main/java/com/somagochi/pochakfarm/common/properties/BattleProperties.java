package com.somagochi.pochakfarm.common.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.battle")
public record BattleProperties(
    Duration restDuration,
    Duration abandonThreshold,
    Duration finalRoundStartTimeout,
    Duration finalRoundDuration,
    Duration finalRoundSubmissionGrace) {}
