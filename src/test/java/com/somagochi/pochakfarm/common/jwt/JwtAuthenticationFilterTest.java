package com.somagochi.pochakfarm.common.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.auth.application.TokenService;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationException;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  private final TokenService tokenService = mock(TokenService.class);
  private final JwtAuthenticationFilter jwtAuthenticationFilter =
      new JwtAuthenticationFilter(tokenService);

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatesRequestWhenBearerTokenIsValid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    given(tokenService.verifyAccessToken("valid-token"))
        .willReturn(
            new JwtPayload(
                "token-1",
                "1",
                Instant.parse("2026-05-26T00:00:00Z"),
                Instant.parse("2026-05-26T01:00:00Z"),
                Map.of("jti", "token-1", "tokenType", "access")));

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
  void clearsAuthenticationAndContinuesChainWhenAccessTokenIsBlacklisted() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer blacklisted-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    given(tokenService.verifyAccessToken("blacklisted-token"))
        .willThrow(new JwtAuthenticationException(ErrorCode.BLACKLISTED_TOKEN));

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void clearsAuthenticationAndContinuesChainWhenBearerTokenIsInvalid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    given(tokenService.verifyAccessToken("invalid-token"))
        .willThrow(new JwtAuthenticationException(ErrorCode.INVALID_TOKEN));

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}
