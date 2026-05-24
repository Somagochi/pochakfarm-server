package com.somagochi.pochakfarm.common.security.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestController {

  @GetMapping("/secure")
  String secure() {
    return "ok";
  }
}
