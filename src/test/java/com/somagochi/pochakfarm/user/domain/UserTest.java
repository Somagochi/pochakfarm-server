package com.somagochi.pochakfarm.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void withdrawMarksDeletedAndAnonymizesUniqueIdentifiers() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    user.withdraw();

    assertTrue(user.isDeleted());
    assertEquals(SocialProvider.KAKAO, user.getSocialAccount().getProvider());
    assertNotEquals("provider-id-1", user.getSocialAccount().getProviderId());
    assertTrue(user.getSocialAccount().getProviderId().startsWith("deleted-"));
    assertTrue(user.getSocialAccount().getProviderId().endsWith("-provider-id-1"));
    assertTrue(user.getEmail().startsWith("deleted-"));
    assertTrue(user.getEmail().endsWith("-test123@test.com"));
  }

  @Test
  void withdrawIsIdempotent() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    user.withdraw();
    String providerIdAfterFirst = user.getSocialAccount().getProviderId();
    String emailAfterFirst = user.getEmail();

    user.withdraw();

    assertEquals(providerIdAfterFirst, user.getSocialAccount().getProviderId());
    assertEquals(emailAfterFirst, user.getEmail());
  }

  @Test
  void withdrawHandlesNullEmail() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", null);

    user.withdraw();

    assertTrue(user.isDeleted());
    assertFalse(user.getSocialAccount().getProviderId().equals("provider-id-1"));
  }
}
