package com.somagochi.pochakfarm.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String CORRELATION_ID = "correlationId";
  public static final String PATH = "path";
  public static final String METHOD = "method";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
    MDC.put(PATH, request.getRequestURI());
    MDC.put(METHOD, request.getMethod());
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CORRELATION_ID);
      MDC.remove(PATH);
      MDC.remove(METHOD);
    }
  }
}
