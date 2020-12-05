package com.github.snqlby.guardbot.handler;

import com.github.snqlby.guardbot.data.ParameterData;
import com.github.snqlby.guardbot.service.AdminService;
import com.github.snqlby.guardbot.service.ParameterService;
import com.github.snqlby.guardbot.service.SessionService;
import com.github.snqlby.guardbot.util.Bot;
import com.github.snqlby.tgwebhook.AcceptTypes;
import com.github.snqlby.tgwebhook.Locality;
import com.github.snqlby.tgwebhook.UpdateType;
import com.github.snqlby.tgwebhook.methods.MessageMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Objects;

@AcceptTypes({UpdateType.MESSAGE})
@Component
public class MessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MessageHandler.class);
  private final SessionService sessionService;
  private final AdminService adminService;
  private final ParameterService parameterService;

  public MessageHandler(SessionService sessionService, AdminService adminService, ParameterService parameterService) {
    this.sessionService = sessionService;
    this.adminService = adminService;
    this.parameterService = parameterService;
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

    if (!Objects.isNull(message.getForwardDate()) && !isForwardedFromSameChat(message)) {
      Boolean deleteForwardEnabled = parameterService.findParameterOrDefault(ParameterData.MODULE_DELETE_FORWARD_MESSAGE_ENABLED, chatId, false);
      if (deleteForwardEnabled && !adminService.isAdmin(chatId, message.getFrom().getId())
        && !hasAllowedWords(message)) {
        try {
          bot.execute(Bot.deleteMessage(message));
        } catch (TelegramApiException e) {
          LOG.error("Message was not deleted. MessageId: {}, ChatId: {}", message.getMessageId(), chatId);
        }
      }
    }

    return null;
  }

  private boolean hasAllowedWords(Message message) {
    String[] allowedWords = parameterService.findParameterOrDefault(ParameterData.MODULE_DELETE_FORWARD_MESSAGE_FILTER, message.getChatId(), new String[]{});
    String text = message.getText();
    if (StringUtils.isEmpty(message.getText())) {
      return false;
    }

    for (String allowedWord : allowedWords) {
      if (text.toLowerCase().contains(allowedWord.toLowerCase())) {
        return true;
      }
    }

    return false;
  }

  private boolean isForwardedFromSameChat(Message message) {
    Long chatId = message.getChatId();

    Chat forwardFromChat = message.getForwardFromChat();
    if (!Objects.isNull(forwardFromChat) && forwardFromChat.getId().equals(chatId)) {
      return true;
    }

    return false;
  }

}
