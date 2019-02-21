package com.github.snqlby.guardbot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

  private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

  @RequestMapping(value = "/", method = RequestMethod.GET)
  @ResponseBody
  public String onUpdateReceived() {
    return "Welcome!";
  }
}
