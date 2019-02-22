package com.github.snqlby.guardbot.handler;

import static com.github.snqlby.guardbot.util.Constants.WORLD_GROUP_ID;

import com.github.snqlby.guardbot.puzzle.Puzzle;
import com.github.snqlby.tgwebhook.AcceptTypes;
import com.github.snqlby.tgwebhook.Locality;
import com.github.snqlby.tgwebhook.UpdateType;
import com.github.snqlby.tgwebhook.methods.CallbackMethod;
import com.github.snqlby.tgwebhook.methods.CallbackOrigin;
import com.github.snqlby.tgwebhook.methods.JoinMethod;
import com.github.snqlby.tgwebhook.methods.JoinReason;
import com.github.snqlby.tgwebhook.methods.LeaveMethod;
import com.github.snqlby.tgwebhook.methods.LeaveReason;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@AcceptTypes({UpdateType.MESSAGE, UpdateType.CALLBACK_QUERY})
@Component
public class JoinLeftHandler {

  private static final Logger LOG = LoggerFactory.getLogger(JoinLeftHandler.class);
  private Map<String, ActivePuzzle> activePuzzles = new ConcurrentHashMap<>();
  private Puzzle puzzle;

  public JoinLeftHandler(Puzzle puzzle) {
    this.puzzle = puzzle;
    puzzle.setComplexity(4);
  }

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
    var users = findJoinedUsers(reason, message);
    User user = users.get(0);
    Integer userId = user.getId();
    muteUser(bot, message.getChatId(), userId);

    Integer joinMessageId = message.getMessageId();
    SendMessage puzzleMessage = (SendMessage) puzzle.nextPuzzle(message);
    puzzleMessage
        .setChatId(message.getChatId())
        .setReplyToMessageId(joinMessageId)
        .enableMarkdown(true)
        .disableNotification();
    Integer puzzleMessageId;
    try {
      puzzleMessageId = bot.execute(puzzleMessage).getMessageId();
      activePuzzles.put(key(message.getChatId(), userId),
          new ActivePuzzle(joinMessageId, puzzleMessageId));
    } catch (TelegramApiException e) {
      LOG.error("Cannot send puzzle to user {}", userId);
    }
    return null;
  }

  @CallbackMethod(data = "banme", origin = CallbackOrigin.MESSAGE, locality = Locality.SUPERGROUP)
  public BotApiMethod onBanMe(AbsSender bot, CallbackQuery query, CallbackOrigin origin) {
    Integer userId = query.getFrom().getId();
    Long chatId = query.getMessage().getChatId();
    if (!hasAccess(userId, chatId, query.getMessage().getMessageId())) {
      return accessDeniedMessage(query.getId());
    }

    LOG.info("Captcha hasn't been resolved by {}", userId);

    String key = key(chatId, userId);
    ActivePuzzle info = activePuzzles.get(key);
    kickUser(bot, query.getMessage().getChatId(), userId);
    removeMessages(bot, chatId, info);
    activePuzzles.remove(key);

    return null;
  }

  @CallbackMethod(data = "solve", origin = CallbackOrigin.MESSAGE, locality = Locality.SUPERGROUP)
  public BotApiMethod onSolve(AbsSender bot, CallbackQuery query, CallbackOrigin origin) {
    Integer userId = query.getFrom().getId();
    Long chatId = query.getMessage().getChatId();
    if (!hasAccess(userId, chatId, query.getMessage().getMessageId())) {
      return accessDeniedMessage(query.getId());
    }

    if (puzzle.hasNext()) {
      try {
        EditMessageText newChallenge = (EditMessageText) puzzle.nextPuzzle(query);
        newChallenge
            .setChatId(chatId)
            .setMessageId(query.getMessage().getMessageId())
            .enableMarkdown(true);
        bot.execute(newChallenge);
        return successMessage(query.getId());
      } catch (TelegramApiException e) {
        LOG.error("Cannot generate a new puzzle, complete it");
      }
    }

    LOG.info("Captcha has been resolved by {}", userId);

    String key = key(chatId, userId);
    removeMessages(bot, chatId, activePuzzles.get(key));
    activePuzzles.remove(key);

    return unmuteUser(chatId, userId);
  }

  private BotApiMethod accessDeniedMessage(String queryId) {
    return createCallbackMessage(queryId, "It's Not Your Fight Anymore");
  }

  private BotApiMethod successMessage(String queryId) {
    return createCallbackMessage(queryId, "Well Done");
  }

  private BotApiMethod createCallbackMessage(String queryId, String text) {
    return new AnswerCallbackQuery().setCallbackQueryId(queryId)
        .setText(text);
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

    String key = key(message.getChatId(), userId);
    if (activePuzzles.containsKey(key)) {
      removeMessages(bot, message.getChatId(), activePuzzles.get(key));
      activePuzzles.remove(key);
    }

    return null;
  }

  private boolean hasAccess(Integer userId, Long chatId, Integer messageId) {
    String key = key(chatId, userId);
    if (!activePuzzles.containsKey(key)) {
      return false;
    } else {
      ActivePuzzle puzzle = activePuzzles.get(key);
      return puzzle.getPuzzleMessageId().equals(messageId);
    }
  }

  private void removeMessages(AbsSender bot, long chatId, ActivePuzzle puzzle) {
    try {
      bot.execute(deleteMessage(chatId, puzzle.getJoinMessageId()));
      bot.execute(deleteMessage(chatId, puzzle.getPuzzleMessageId()));
    } catch (TelegramApiException e) {
      LOG.warn("Cannot remove a message: {}. {}", puzzle, e.getMessage());
    }
  }

  private List<org.telegram.telegrambots.meta.api.objects.User> findJoinedUsers(
      JoinReason reason, Message message) {
    if (reason == JoinReason.ADD) {
      return message.getNewChatMembers();
    } else {
      return List.of(message.getFrom());
    }
  }

  private BotApiMethod<?> deleteMessage(long chatId, int messageId) {
    return new DeleteMessage(chatId, messageId);
  }

  private BotApiMethod<?> deleteMessage(Message message) {
    return deleteMessage(message.getChatId(), message.getMessageId());
  }

  private boolean kickUser(AbsSender bot, long chatId, int userId) {
    final int banDuration = 60;
    try {
      bot.execute(
          new KickChatMember(chatId, userId)
              .forTimePeriod(Duration.ofSeconds(banDuration)));
    } catch (TelegramApiException e) {
      LOG.error("Cannot execute KickChatMember method: {}", e.getMessage());
      return false;
    }
    return true;
  }

  private boolean muteUser(AbsSender bot, long groupId, int userId) {
    try {
      bot.execute(new RestrictChatMember(groupId, userId).setCanSendMessages(false));
    } catch (TelegramApiException e) {
      LOG.error("Cannot execute KickChatMember method: {}", e.getMessage());
      return false;
    }
    return true;
  }

  private BotApiMethod unmuteUser(long chatId, int userId) {
    return new RestrictChatMember(chatId, userId)
        .setCanSendMessages(true)
        .setCanAddWebPagePreviews(true)
        .setCanSendMediaMessages(true)
        .setCanSendOtherMessages(true);
  }

  private String key(Long chatId, Integer userId) {
    return chatId.toString() + userId.toString();
  }
}
