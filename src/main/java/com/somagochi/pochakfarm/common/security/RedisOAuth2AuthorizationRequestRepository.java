package com.somagochi.pochakfarm.common.security;

import com.somagochi.pochakfarm.common.properties.OAuth2LoginProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RedisOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String KEY_PREFIX = "auth:oauth2:authorization-request:";

  private static final RedisScript<String> CONSUME_SCRIPT =
      RedisScript.of(
          "local value = redis.call('get', KEYS[1]); "
              + "if value then redis.call('del', KEYS[1]) end; "
              + "return value",
          String.class);

  private final StringRedisTemplate redisTemplate;
  private final JsonMapper jsonMapper;
  private final Duration authorizationRequestTtl;

  public RedisOAuth2AuthorizationRequestRepository(
      StringRedisTemplate redisTemplate, OAuth2LoginProperties oAuth2LoginProperties) {
    this.redisTemplate = redisTemplate;
    this.jsonMapper = authorizationRequestMapper();
    this.authorizationRequestTtl = requirePositive(oAuth2LoginProperties.authorizationRequestTtl());
  }

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    String state = stateOf(request);
    if (state == null) {
      return null;
    }
    return deserialize(redisTemplate.opsForValue().get(key(state)));
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authorizationRequest == null) {
      removeAuthorizationRequest(request, response);
      return;
    }
    String state = authorizationRequest.getState();
    if (state == null || state.isBlank()) {
      throw new IllegalArgumentException("authorizationRequest.state must not be blank");
    }
    redisTemplate
        .opsForValue()
        .set(
            key(state),
            jsonMapper.writeValueAsString(authorizationRequest),
            authorizationRequestTtl);
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    String state = stateOf(request);
    if (state == null) {
      return null;
    }
    return deserialize(redisTemplate.execute(CONSUME_SCRIPT, List.of(key(state))));
  }

  private String stateOf(HttpServletRequest request) {
    String state = request.getParameter(OAuth2ParameterNames.STATE);
    return state == null || state.isBlank() ? null : state;
  }

  private OAuth2AuthorizationRequest deserialize(String payload) {
    if (payload == null) {
      return null;
    }
    try {
      return jsonMapper.readValue(payload, OAuth2AuthorizationRequest.class);
    } catch (JacksonException exception) {
      return null;
    }
  }

  private String key(String state) {
    return KEY_PREFIX + state;
  }

  private static JsonMapper authorizationRequestMapper() {
    return JsonMapper.builder()
        .addModules(
            SecurityJacksonModules.getModules(
                RedisOAuth2AuthorizationRequestRepository.class.getClassLoader()))
        .build();
  }

  private static Duration requirePositive(Duration ttl) {
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("authorizationRequestTtl must be greater than zero");
    }
    return ttl;
  }
}
