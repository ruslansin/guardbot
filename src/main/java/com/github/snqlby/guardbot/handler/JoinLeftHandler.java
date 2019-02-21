package com.github.snqlby.guardbot.handler;

import static com.github.snqlby.guardbot.util.Constants.WORLD_GROUP_ID;

import com.github.snqlby.tgwebhook.AcceptTypes;
import com.github.snqlby.tgwebhook.methods.JoinMethod;
import com.github.snqlby.tgwebhook.methods.JoinReason;
import com.github.snqlby.tgwebhook.methods.LeaveMethod;
import com.github.snqlby.tgwebhook.methods.LeaveReason;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;

@AcceptTypes
@Component
public class JoinLeftHandler {

  private static final Logger LOG = LoggerFactory.getLogger(JoinLeftHandler.class);

  /**
   * Process an event when user has joined the group.
   *
   * @param bot bot instance
   * @param message received message
   * @param reason reason for the join
   * @return null if user doesn't exists
   */
  @JoinMethod(
      room = WORLD_GROUP_ID,
      reason = {JoinReason.SELF}
  )
  public BotApiMethod onUserJoinedSelf(AbsSender bot, Message message, JoinReason reason) {
    var user = findJoinedUsers(reason, message);

    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      List<InlineKeyboardButton> row = new ArrayList<>();
      for (int j = 0; j < 5; j++) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        String id = String.valueOf(j);
        button.setText(id).setCallbackData(id);
        row.add(button);
      }
      keyboard.add(row);
    }

    return new SendMessage(WORLD_GROUP_ID, "join message")
        .setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(keyboard))
        .setReplyToMessageId(message.getMessageId()).enableMarkdown(true);
  }

  private List<org.telegram.telegrambots.meta.api.objects.User> findJoinedUsers(
      JoinReason reason, Message message) {
    if (reason == JoinReason.ADD) {
      return message.getNewChatMembers();
    } else {
      return List.of(message.getFrom());
    }
  }

  /**
   * Process all leave messages.
   *
   * @param bot bot instance
   * @param message received message
   * @param reason reason for the leave (kicked, leaved himself)
   * @return null if user has left the channel but was offline (api sometimes dosn't send all
   * requests), RestrictChatMember instance otherwise
   */
  @LeaveMethod(
      room = WORLD_GROUP_ID,
      reason = {LeaveReason.KICK, LeaveReason.SELF}
  )
  public BotApiMethod onUserLeft(AbsSender bot, Message message, LeaveReason reason) {
    User firstUser = message.getLeftChatMember();
    int userId = reason == LeaveReason.SELF ? message.getFrom().getId() : firstUser.getId();

    try {
      bot.execute(deleteMessage(message));
    } catch (TelegramApiException e) {
      LOG.warn("Cannot remove a message: {}. {}", userId, e.getMessage());
    }

    return null;
  }

  private BotApiMethod<?> deleteMessage(Message message) {
    return new DeleteMessage(message.getChatId(), message.getMessageId());
  }

  private boolean kickUser(AbsSender bot, int userId) {
    final int banDuration = 35;
    try {
      bot.execute(
          new KickChatMember(WORLD_GROUP_ID, userId)
              .setUntilDate(
                  (int)
                      (System.currentTimeMillis() / Duration.ofSeconds(1).toMillis()
                          + banDuration)));
    } catch (TelegramApiException e) {
      LOG.error("Cannot execute KickChatMember method: {}", e.getMessage());
      return false;
    }
    return true;
  }

  private boolean muteUser(AbsSender bot, int userId) {
    try {
      bot.execute(new RestrictChatMember(WORLD_GROUP_ID, userId).setCanSendMessages(false));
    } catch (TelegramApiException e) {
      LOG.error("Cannot execute KickChatMember method: {}", e.getMessage());
      return false;
    }
    return true;
  }

  private BotApiMethod unmuteUser(int userId) {
    return new RestrictChatMember(WORLD_GROUP_ID, userId)
        .setCanSendMessages(true)
        .setCanAddWebPagePreviews(true)
        .setCanSendMediaMessages(true)
        .setCanSendOtherMessages(true);
  }
}
