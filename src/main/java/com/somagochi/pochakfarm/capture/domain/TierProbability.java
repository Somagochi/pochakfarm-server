package com.somagochi.pochakfarm.capture.domain;

import java.math.BigDecimal;

public record TierProbability(Tier tier, BigDecimal probabilityPercent) {}
