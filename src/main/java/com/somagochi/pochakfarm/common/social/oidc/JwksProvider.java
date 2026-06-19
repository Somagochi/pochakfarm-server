package com.somagochi.pochakfarm.common.social.oidc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.JwksCacheProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import java.security.Key;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class JwksProvider {

  private static final int MAX_JWKS_SOURCES = 16;

  private final RestClient restClient = RestClient.builder().build();
  private final JwksCacheProperties properties;
  private final Cache<String, JwkBundle> cache;

  public JwksProvider(JwksCacheProperties properties) {
    this.properties = properties;
    this.cache =
        Caffeine.newBuilder().expireAfterWrite(properties.ttl()).maximumSize(MAX_JWKS_SOURCES).build();
  }

  public Key findKey(String jwksUri, String kid) {
    JwkBundle bundle =
        cache
            .asMap()
            .compute(
                jwksUri,
                (uri, current) -> {
                  if (current == null || expired(current)) {
                    return fetch(uri);
                  }
                  if (current.contains(kid)) {
                    return current;
                  }
                  if (!refreshable(current)) {
                    return current;
                  }
                  return fetch(uri);
                });
    Key key = bundle.key(kid);
    if (key == null) {
      throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
    }
    return key;
  }

  private boolean expired(JwkBundle bundle) {
    return Instant.now().isAfter(bundle.fetchedAt().plus(properties.ttl()));
  }

  private boolean refreshable(JwkBundle bundle) {
    return Instant.now().isAfter(bundle.fetchedAt().plus(properties.minRefreshInterval()));
  }

  private JwkBundle fetch(String jwksUri) {
    try {
      String body = restClient.get().uri(jwksUri).retrieve().body(String.class);
      JwkSet jwkSet = Jwks.setParser().build().parse(body);
      Map<String, Key> keys = new HashMap<>();
      for (Jwk<?> jwk : jwkSet.getKeys()) {
        keys.put(jwk.getId(), jwk.toKey());
      }
      return new JwkBundle(Map.copyOf(keys), Instant.now());
    } catch (RestClientException | JwtException exception) {
      throw new BusinessException(ErrorCode.SOCIAL_USER_INFO_FAILED);
    }
  }

  private record JwkBundle(Map<String, Key> keys, Instant fetchedAt) {

    boolean contains(String kid) {
      return keys.containsKey(kid);
    }

    Key key(String kid) {
      return keys.get(kid);
    }
  }
}
