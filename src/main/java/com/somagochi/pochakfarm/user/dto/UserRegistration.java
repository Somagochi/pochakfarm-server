package com.somagochi.pochakfarm.user.dto;

import com.somagochi.pochakfarm.user.domain.User;

public record UserRegistration(User user, boolean isNew) {}
