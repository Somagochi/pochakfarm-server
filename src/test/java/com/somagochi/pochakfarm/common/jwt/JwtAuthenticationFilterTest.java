package com.somagochi.pochakfarm.common.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.auth.application.TokenService;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationException;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationToken;
import com.somagochi.pochakfarm.user.application.UserService;
import com.somagochi.pochakfarm.user.domain.User;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  private final TokenService tokenService = org.mockito.Mockito.mock(TokenService.class);
  private final UserService userService = org.mockito.Mockito.mock(UserService.class);
  private final JwtAuthenticationFilter jwtAuthenticationFilter =
      new JwtAuthenticationFilter(tokenService, userService);

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatesRequestWhenBearerTokenIsValid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    given(tokenService.parseAccessToken("valid-token"))
        .willReturn(
            new JwtPayload(
                "token-1",
                "1",
                Instant.parse("2026-05-26T00:00:00Z"),
                Instant.parse("2026-05-26T01:00:00Z"),
                Map.of("jti", "token-1", "tokenType", "access")));
    given(userService.getById(1L)).willReturn(new User(1L));

    jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

    assertTrue(
        SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken);
    assertEquals("1", SecurityContextHolder.getContext().getAuthentication().getName());
    JwtAuthenticationToken authentication =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    assertEquals("valid-token", authentication.getCredentials());
    assertEquals("token-1", authentication.getPayload().tokenId());
    assertEquals(1L, authentication.getPrincipal().id());
  }

  @Test
  void delegatesExceptionResolutionWhenBearerTokenIsInvalid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    JwtAuthenticationException exception = new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);

    given(tokenService.parseAccessToken("invalid-token")).willThrow(exception);

    JwtAuthenticationException thrown =
        assertThrows(
            JwtAuthenticationException.class,
            () -> jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain()));

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(ErrorCode.INVALID_TOKEN.getCode(), thrown.getCode());
  }
}
