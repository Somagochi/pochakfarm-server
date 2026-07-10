package com.somagochi.pochakfarm.common.jwt;

public class JwtInvalidException extends RuntimeException {

  public JwtInvalidException() {}

  public JwtInvalidException(Throwable cause) {
    super(cause);
  }
}
