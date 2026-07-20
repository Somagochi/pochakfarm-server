package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.security.JwtAuthenticationFilter;
import com.somagochi.pochakfarm.common.security.OAuth2LoginSuccessHandler;
import com.somagochi.pochakfarm.common.security.OAuth2UserServiceImpl;
import com.somagochi.pochakfarm.common.security.SecurityAccessDeniedHandler;
import com.somagochi.pochakfarm.common.security.SecurityAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private static final String[] PUBLIC_GET_ENDPOINTS = {
    "/actuator/health",
    "/actuator/health/**",
    "/actuator/prometheus",
    "/api/characterizations/public/*",
    "/api/auth/oauth2/**",
    "/share/**"
  };

  private static final String[] PUBLIC_POST_ENDPOINTS = {
    "/api/auth/login",
    "/api/characterizations/public",
    "/api/pre-registrations",
    "/api/auth/refresh"
  };

  private static final String[] PUBLIC_DELETE_ENDPOINTS = {"/api/pre-registrations"};

  private final SecurityAuthenticationEntryPoint authenticationEntryPoint;
  private final SecurityAccessDeniedHandler accessDeniedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
      ObjectProvider<OAuth2UserServiceImpl> oauth2UserService,
      ObjectProvider<OAuth2LoginSuccessHandler> oauth2LoginSuccessHandler)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS)
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS)
                    .permitAll()
                    .requestMatchers(HttpMethod.DELETE, PUBLIC_DELETE_ENDPOINTS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler));

    if (clientRegistrationRepository.getIfAvailable() != null) {
      http.oauth2Login(
          oauth ->
              oauth
                  .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2"))
                  .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2/code/*"))
                  .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService.getObject()))
                  .successHandler(oauth2LoginSuccessHandler.getObject()));
    }

    return http.build();
  }
}
