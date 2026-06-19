package com.somagochi.pochakfarm.common.security;

import java.security.Principal;

public record UserPrincipal(Long id) implements Principal {

  @Override
  public String getName() {
    return String.valueOf(id);
  }
}
