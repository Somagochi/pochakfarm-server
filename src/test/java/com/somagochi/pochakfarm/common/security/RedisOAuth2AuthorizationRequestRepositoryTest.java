package com.somagochi.pochakfarm.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.common.properties.OAuth2LoginProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class RedisOAuth2AuthorizationRequestRepositoryTest {

  private static final String STATE = "state-value";
  private static final String KEY = "auth:oauth2:authorization-request:" + STATE;
  private static final Duration TTL = Duration.ofMinutes(3);

  private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

  private final RedisOAuth2AuthorizationRequestRepository repository =
      new RedisOAuth2AuthorizationRequestRepository(
          redisTemplate, new OAuth2LoginProperties("https://front.example.com/callback", TTL));

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @Test
  void savesSerializedAuthorizationRequestKeyedByStateWithTtl() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);

    repository.saveAuthorizationRequest(
        authorizationRequest(), new MockHttpServletRequest(), response);

    verify(valueOperations).set(eq(KEY), payload.capture(), eq(TTL));
    assertTrue(payload.getValue().contains(STATE), payload.getValue());
  }

  @Test
  void restoresEveryFieldNeededToCompleteTheLoginRoundTrip() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
    repository.saveAuthorizationRequest(
        authorizationRequest(), new MockHttpServletRequest(), response);
    verify(valueOperations).set(eq(KEY), payload.capture(), eq(TTL));
    given(valueOperations.get(KEY)).willReturn(payload.getValue());

    OAuth2AuthorizationRequest loaded =
        repository.loadAuthorizationRequest(requestWithState(STATE));

    assertEquals(STATE, loaded.getState());
    assertEquals("com.example.app.web", loaded.getClientId());
    assertEquals("https://api.example.com/api/auth/oauth2/code/apple", loaded.getRedirectUri());
    assertEquals("https://appleid.apple.com/auth/authorize", loaded.getAuthorizationUri());
    assertTrue(loaded.getScopes().contains("email"));
    assertEquals("form_post", loaded.getAdditionalParameters().get("response_mode"));
    assertEquals("apple", loaded.getAttribute("registration_id"));
  }

  @Test
  void removesWithSingleAtomicScriptSoCodeCannotBeReplayed() {
    given(redisTemplate.execute(any(RedisScript.class), anyList())).willReturn(null);

    repository.removeAuthorizationRequest(requestWithState(STATE), response);

    ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
    verify(redisTemplate).execute(any(RedisScript.class), keys.capture());
    assertEquals(List.of(KEY), keys.getValue());
  }

  @Test
  void savingNullRemovesStoredAuthorizationRequest() {
    given(redisTemplate.execute(any(RedisScript.class), anyList())).willReturn(null);

    repository.saveAuthorizationRequest(null, requestWithState(STATE), response);

    verify(redisTemplate).execute(any(RedisScript.class), anyList());
    verify(redisTemplate, never()).opsForValue();
  }

  @Test
  void returnsNullWhenRequestCarriesNoState() {
    assertNull(repository.loadAuthorizationRequest(new MockHttpServletRequest()));
    assertNull(repository.removeAuthorizationRequest(new MockHttpServletRequest(), response));
    verify(redisTemplate, never()).opsForValue();
  }

  @Test
  void returnsNullWhenStoredPayloadIsCorrupted() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get(KEY)).willReturn("not-json");

    assertNull(repository.loadAuthorizationRequest(requestWithState(STATE)));
  }

  @Test
  void rejectsNonPositiveTtl() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RedisOAuth2AuthorizationRequestRepository(
                redisTemplate,
                new OAuth2LoginProperties("https://front.example.com", Duration.ZERO)));
  }

  private static MockHttpServletRequest requestWithState(String state) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("state", state);
    return request;
  }

  private static OAuth2AuthorizationRequest authorizationRequest() {
    return OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri("https://appleid.apple.com/auth/authorize")
        .clientId("com.example.app.web")
        .redirectUri("https://api.example.com/api/auth/oauth2/code/apple")
        .scope("email")
        .state(STATE)
        .additionalParameters(Map.of("response_mode", "form_post"))
        .attributes(Map.of("registration_id", "apple"))
        .build();
  }
}
