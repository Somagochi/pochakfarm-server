package com.somagochi.pochakfarm.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void putsCorrelationContextInMdcDuringRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    MockHttpServletResponse response = new MockHttpServletResponse();
    String[] captured = new String[3];

    FilterChain chain =
        (req, res) -> {
          captured[0] = MDC.get(CorrelationIdFilter.CORRELATION_ID);
          captured[1] = MDC.get(CorrelationIdFilter.PATH);
          captured[2] = MDC.get(CorrelationIdFilter.METHOD);
        };

    filter.doFilter(request, response, chain);

    assertNotNull(captured[0]);
    assertEquals("/api/auth/login", captured[1]);
    assertEquals("POST", captured[2]);
  }

  @Test
  void clearsMdcAfterRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/foo");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID));
    assertNull(MDC.get(CorrelationIdFilter.PATH));
    assertNull(MDC.get(CorrelationIdFilter.METHOD));
  }

  @Test
  void clearsMdcEvenWhenChainThrows() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/foo");
    MockHttpServletResponse response = new MockHttpServletResponse();

    try {
      filter.doFilter(
          request,
          response,
          (req, res) -> {
            throw new RuntimeException("boom");
          });
    } catch (Exception ignored) {
      // 예외가 나도 finally 에서 MDC 가 정리되어야 한다.
    }

    assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID));
  }
}
