package com.github.snqlby.guardbot.controller;

import com.github.snqlby.guardbot.service.TelegramBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookController {

  private static final Logger LOG = LoggerFactory.getLogger(WebhookController.class);

  private TelegramBotService botService;

  public WebhookController(TelegramBotService botService) {
    this.botService = botService;
  }

  @RequestMapping(value = "/${telegram.token}", method = RequestMethod.POST)
  @ResponseBody
  public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
    LOG.debug(update.toString());
    return botService.onWebhookUpdateReceived(update);
  }
}
