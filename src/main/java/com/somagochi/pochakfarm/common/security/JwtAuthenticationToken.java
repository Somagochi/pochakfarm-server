package com.somagochi.pochakfarm.common.security;

import com.somagochi.pochakfarm.common.jwt.JwtPayload;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

  private final String token;
  private final UserPrincipal principal;
  @Getter private final JwtPayload payload;

  public JwtAuthenticationToken(String token, UserPrincipal principal, JwtPayload payload) {
    this(token, principal, payload, Collections.emptyList());
  }

  public JwtAuthenticationToken(
      String token,
      UserPrincipal principal,
      JwtPayload payload,
      Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.token = token;
    this.principal = principal;
    this.payload = payload;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public UserPrincipal getPrincipal() {
    return principal;
  }
}
