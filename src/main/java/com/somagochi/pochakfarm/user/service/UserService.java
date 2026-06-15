package com.somagochi.pochakfarm.user.service;

import com.somagochi.pochakfarm.user.domain.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  public User getById(Long userId) {
    // TODO: replace with a repository-backed lookup once the user persistence model is ready.
    return new User(userId);
  }
}
