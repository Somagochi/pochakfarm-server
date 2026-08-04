package com.somagochi.pochakfarm.develop.config;

import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@Profile({"local", "dev"})
public class DevelopSecurityConfig {

  private static final String[] AUTHENTICATED_ENDPOINTS = {"/api/dev/animal"};
  private static final String[] BASIC_AUTH_ENDPOINTS = {
    "/api/dev/achievements", "/api/dev/achievements/**"
  };

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final SecurityAuthenticationEntryPoint authenticationEntryPoint;

  public DevelopSecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      SecurityAuthenticationEntryPoint authenticationEntryPoint) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/dev/**", "/", "/pre-registration.html", "/social-login-test.html")
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(AUTHENTICATED_ENDPOINTS)
                    .authenticated()
                    .requestMatchers(BASIC_AUTH_ENDPOINTS)
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            ex ->
                ex.defaultAuthenticationEntryPointFor(
                        basicAuthenticationEntryPoint(), basicAuthEndpointsMatcher())
                    .defaultAuthenticationEntryPointFor(
                        authenticationEntryPoint, AnyRequestMatcher.INSTANCE));
    return http.build();
  }

  private BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
    BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName("pochakfarm-dev");
    return entryPoint;
  }

  private OrRequestMatcher basicAuthEndpointsMatcher() {
    PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher.withDefaults();
    return new OrRequestMatcher(
        builder.matcher("/api/dev/achievements"), builder.matcher("/api/dev/achievements/**"));
  }
}
