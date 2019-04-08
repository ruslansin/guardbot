package com.github.snqlby.guardbot.handler;

import com.github.snqlby.guardbot.service.SessionService;
import com.github.snqlby.guardbot.util.Bot;
import com.github.snqlby.tgwebhook.AcceptTypes;
import com.github.snqlby.tgwebhook.Locality;
import com.github.snqlby.tgwebhook.UpdateType;
import com.github.snqlby.tgwebhook.methods.MessageMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

@AcceptTypes({UpdateType.MESSAGE})
@Component
public class MessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MessageHandler.class);
  private SessionService sessionService;

  public MessageHandler(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @MessageMethod(
      locality = {Locality.GROUP, Locality.SUPERGROUP}
  )
  public BotApiMethod onMessage(AbsSender bot, Message message) {
    Long chatId = message.getChatId();
    Integer userId = message.getFrom().getId();
    if (sessionService.isAlive(chatId, userId)) {
      LOG.info("Remove a message for chat {} for user {}", chatId, userId);
      return Bot.deleteMessage(chatId, message.getMessageId());
    }
    return null;
  }
}
