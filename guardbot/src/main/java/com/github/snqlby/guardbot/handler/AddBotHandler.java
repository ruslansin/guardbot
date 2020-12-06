package com.github.snqlby.guardbot.handler;

import static com.github.snqlby.guardbot.util.Constants.ROOT_ADMIN_ID;

import com.github.snqlby.guardbot.config.TelegramBotConfig;
import com.github.snqlby.tgwebhook.AcceptTypes;
import com.github.snqlby.tgwebhook.UpdateType;
import com.github.snqlby.tgwebhook.methods.JoinMethod;
import com.github.snqlby.tgwebhook.methods.JoinReason;
import java.util.List;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

@AcceptTypes({UpdateType.MESSAGE})
@Component
public class AddBotHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AddBotHandler.class);
  private final TelegramBotConfig botConfig;

  public AddBotHandler(@Qualifier("guardbotConfig") TelegramBotConfig botConfig) {
    this.botConfig = botConfig;
  }

  /**
   * Process an event when bot has added to the group.
   *
   * @param bot bot instance
   * @param message received message
   * @param reason reason for the join
   * @return null if user doesn't exists
   */
  @JoinMethod(
      reason = {JoinReason.ADD}
  )
  public BotApiMethod onBotAdded(AbsSender bot, Message message, JoinReason reason) {
    if (!hasBot(message.getNewChatMembers(), botConfig.getUsername())) {
      return null;
    }
    LOG.info("Bot was added to {} by {}", message.getChatId(), message.getFrom().getId());
    return new SendMessage((long) ROOT_ADMIN_ID, generateReply(message)).enableMarkdown(true);
  }

  private boolean hasBot(List<User> users, String botUsername) {
    if (users == null || botUsername == null) {
      return false;
    }
    return users.stream().anyMatch(u -> botUsername.equals(u.getUserName()));
  }

  private String generateReply(Message message) {
    Chat chat = message.getChat();
    User user = message.getFrom();
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add("Bot was added!");
    joiner.add(String.format("*Chat:* `%s (%d) [%s]`",
        chat.getTitle(),
        chat.getId(),
        chat.getUserName()));
    joiner.add(String.format("*Added by:* `%s %s (%d) [%s] |%s|`",
        user.getFirstName(),
        user.getLastName(),
        user.getId(),
        user.getUserName(),
        user.getLanguageCode()));
    return joiner.toString();
  }


}
