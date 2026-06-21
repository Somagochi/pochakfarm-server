package com.somagochi.pochakfarm.common.social;

public interface SocialLoginClient {

  boolean supports(SocialProvider provider);

  SocialUserInfo authenticate(String token);
}
