package com.somagochi.pochakfarm.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.common.properties.OAuth2LoginProperties;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.util.ServletRequestPathUtils;

class CorsConfigTest {

  private static final String SUCCESS_REDIRECT_URI = "https://front.example.com/coupon";
  private static final String APPLE_ORIGIN = "https://appleid.apple.com";
  private static final String APPLE_CALLBACK_PATH = "/api/auth/oauth2/code/apple";

  private final CorsConfigurationSource corsConfigurationSource =
      new CorsConfig()
          .corsConfigurationSource(
              new OAuth2LoginProperties(SUCCESS_REDIRECT_URI, Duration.ofMinutes(3)));

  private final CorsProcessor corsProcessor = new DefaultCorsProcessor();

  @Test
  void allowsAppleFormPostCallback() throws IOException {
    MockHttpServletRequest request = request("POST", APPLE_CALLBACK_PATH, APPLE_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(process(request, response));
    assertEquals(APPLE_ORIGIN, response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void allowsAppleCallbackPreflight() throws IOException {
    MockHttpServletRequest request = request("OPTIONS", APPLE_CALLBACK_PATH, APPLE_ORIGIN);
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(process(request, response));
    assertEquals(APPLE_ORIGIN, response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void allowsCallbackWithoutOriginHeader() throws IOException {
    MockHttpServletRequest request = request("POST", APPLE_CALLBACK_PATH, null);
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(process(request, response));
  }

  @Test
  void doesNotExposeAppleOriginOnOtherEndpoints() throws IOException {
    MockHttpServletRequest request = request("POST", "/api/auth/login", APPLE_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertFalse(process(request, response));
    assertEquals(403, response.getStatus());
  }

  @Test
  void allowsConfiguredFrontendOrigin() throws IOException {
    MockHttpServletRequest request =
        request("POST", "/api/auth/login", "https://front.example.com");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(process(request, response));
    assertEquals(
        "https://front.example.com", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  private boolean process(MockHttpServletRequest request, MockHttpServletResponse response)
      throws IOException {
    return corsProcessor.processRequest(
        corsConfigurationSource.getCorsConfiguration(request), request, response);
  }

  private MockHttpServletRequest request(String method, String path, String origin) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRequestURI(path);
    if (origin != null) {
      request.addHeader(HttpHeaders.ORIGIN, origin);
    }
    ServletRequestPathUtils.parseAndCache(request);
    return request;
  }
}
