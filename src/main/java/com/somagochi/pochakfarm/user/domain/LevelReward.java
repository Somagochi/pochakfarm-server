package com.somagochi.pochakfarm.user.domain;

public record LevelReward(
    long experienceReward,
    int levelBefore,
    int levelAfter,
    long experienceAfter,
    long requiredExperienceForNextLevel,
    long coinReward) {}
