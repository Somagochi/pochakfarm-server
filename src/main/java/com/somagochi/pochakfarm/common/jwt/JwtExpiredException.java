package com.somagochi.pochakfarm.common.jwt;

public class JwtExpiredException extends JwtInvalidException {

  public JwtExpiredException() {}

  public JwtExpiredException(Throwable cause) {
    super(cause);
  }
}
