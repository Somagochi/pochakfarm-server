package com.somagochi.pochakfarm.user.domain;

import java.security.Principal;

public record User(Long id) implements Principal {

  @Override
  public String getName() {
    return String.valueOf(id);
  }
}
