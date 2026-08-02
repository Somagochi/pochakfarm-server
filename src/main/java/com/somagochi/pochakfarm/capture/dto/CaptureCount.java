package com.somagochi.pochakfarm.capture.dto;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;

public record CaptureCount(CardType cardType, Tier tier, long count) {}
