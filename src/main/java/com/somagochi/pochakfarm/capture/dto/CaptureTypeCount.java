package com.somagochi.pochakfarm.capture.dto;

import com.somagochi.pochakfarm.characterization.domain.CardType;

public record CaptureTypeCount(CardType cardType, long count) {}
