package com.github.snqlby.guardbot.util;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot {

  private static final Logger LOG = LoggerFactory.getLogger(Bot.class);

  public static BotApiMethod createCallbackMessage(String queryId, String text) {
    return new AnswerCallbackQuery().setCallbackQueryId(queryId)
        .setText(text);
  }

  public static BotApiMethod<?> deleteMessage(long chatId, int messageId) {
    return new DeleteMessage(chatId, messageId);
  }

  public static BotApiMethod<?> deleteMessage(Message message) {
    return deleteMessage(message.getChatId(), message.getMessageId());
  }

  public static boolean kickUser(AbsSender bot, long chatId, int userId, int duration) {
    LOG.debug("Kicking user {} for {} seconds from {}", userId, duration, chatId);
    try {
      bot.execute(
          new KickChatMember(chatId, userId)
              .forTimePeriod(Duration.ofSeconds(duration)));
    } catch (TelegramApiException e) {
      LOG.error("Cannot execute KickChatMember method: {}", e.getMessage());
      return false;
    }
    return true;
  }

  public static boolean muteUser(AbsSender bot, long groupId, int userId) {
    try {
      bot.execute(new RestrictChatMember(groupId, userId).setCanSendMessages(false));
    } catch (TelegramApiException e) {
      LOG.error("Cannot execute KickChatMember method: {}", e.getMessage());
      return false;
    }
    return true;
  }

  public static BotApiMethod unmuteUser(long chatId, int userId) {
    return new RestrictChatMember(chatId, userId)
        .setCanSendMessages(true)
        .setCanAddWebPagePreviews(true)
        .setCanSendMediaMessages(true)
        .setCanSendOtherMessages(true);
  }

}
