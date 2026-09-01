package com.somagochi.pochakfarm.common.social.oidc;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import java.security.Key;
import java.util.Collection;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OidcVerifier {

  private final JwksProvider jwksProvider;

  public OidcVerifier(JwksProvider jwksProvider) {
    this.jwksProvider = jwksProvider;
  }

  public Claims verify(
      String idToken, String issuer, Collection<String> audiences, String jwksUri) {
    try {
      Claims claims =
          Jwts.parser()
              .keyLocator(new JwksKeyLocator(jwksUri))
              .requireIssuer(issuer)
              .build()
              .parseSignedClaims(idToken)
              .getPayload();
      validateAudience(claims, audiences);
      return claims;
    } catch (JwtException | IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
    }
  }

  private void validateAudience(Claims claims, Collection<String> audiences) {
    Set<String> aud = claims.getAudience();
    if (aud == null || audiences == null || audiences.stream().noneMatch(aud::contains)) {
      throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
    }
  }

  private final class JwksKeyLocator extends LocatorAdapter<Key> {

    private final String jwksUri;

    private JwksKeyLocator(String jwksUri) {
      this.jwksUri = jwksUri;
    }

    @Override
    protected Key locate(JwsHeader header) {
      return jwksProvider.findKey(jwksUri, header.getKeyId());
    }
  }
}
