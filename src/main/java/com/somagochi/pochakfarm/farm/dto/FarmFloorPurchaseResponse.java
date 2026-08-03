package com.somagochi.pochakfarm.farm.dto;

import com.somagochi.pochakfarm.characterization.domain.CardType;

public record FarmFloorPurchaseResponse(CardType type, int unlockedFloor, long remainingCoins) {}
