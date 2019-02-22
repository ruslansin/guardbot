package com.github.snqlby.guardbot.handler;

import static com.github.snqlby.guardbot.util.Constants.PUZZLE_AUTOFAIL_TIME;
import static com.github.snqlby.guardbot.util.Constants.PUZZLE_BAN_TIME;
import static com.github.snqlby.guardbot.util.Constants.PUZZLE_CHALLENGE_TIME;

import com.github.snqlby.guardbot.puzzle.Puzzle;
import com.github.snqlby.guardbot.util.Bot;
import com.github.snqlby.tgwebhook.AcceptTypes;
import com.github.snqlby.tgwebhook.Locality;
import com.github.snqlby.tgwebhook.UpdateType;
import com.github.snqlby.tgwebhook.methods.CallbackMethod;
import com.github.snqlby.tgwebhook.methods.CallbackOrigin;
import com.github.snqlby.tgwebhook.methods.JoinMethod;
import com.github.snqlby.tgwebhook.methods.JoinReason;
import com.github.snqlby.tgwebhook.methods.LeaveMethod;
import com.github.snqlby.tgwebhook.methods.LeaveReason;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
      reason = {JoinReason.SELF}
  )
  public BotApiMethod onUserJoined(AbsSender bot, Message message, JoinReason reason) {
    User user = message.getFrom();
    Integer userId = user.getId();
    Long chatId = message.getChatId();
    Bot.muteUser(bot, chatId, userId);

    Integer joinMessageId = message.getMessageId();
    SendMessage puzzleMessage = (SendMessage) puzzle.nextPuzzle(message);
    puzzleMessage
        .setChatId(chatId)
        .setReplyToMessageId(joinMessageId)
        .enableMarkdown(true)
        .disableNotification();
    Integer puzzleMessageId;
    try {
      puzzleMessageId = bot.execute(puzzleMessage).getMessageId();
      String key = key(chatId, userId);
      Thread puzzleThread = new Thread(() -> {
        try {
          Thread.sleep(PUZZLE_CHALLENGE_TIME * 1000);
        } catch (InterruptedException e) {
          LOG.debug("Task has been interrupted: {}", key);
          return;
        }

        if (activePuzzles.containsKey(key)) {
          LOG.debug("Time to solve the puzzle is out: {}", key);
          ActivePuzzle puzzle = activePuzzles.get(key);
          Bot.kickUser(bot, chatId, userId, PUZZLE_AUTOFAIL_TIME);
          removeMessages(bot, chatId, puzzle);
          activePuzzles.remove(key);
        }
      });
      activePuzzles.put(key, new ActivePuzzle(joinMessageId, puzzleMessageId, puzzleThread));
      puzzleThread.start();
    } catch (TelegramApiException e) {
      LOG.error("Cannot send puzzle to user {}", userId);
    }
    return null;
  }

  @CallbackMethod(
      data = "banme",
      origin = CallbackOrigin.MESSAGE,
      locality = {Locality.GROUP, Locality.SUPERGROUP}
  )
  public BotApiMethod onBanMe(AbsSender bot, CallbackQuery query, CallbackOrigin origin) {
    Integer userId = query.getFrom().getId();
    Long chatId = query.getMessage().getChatId();
    if (!hasAccess(userId, chatId, query.getMessage().getMessageId())) {
      return accessDeniedMessage(query.getId());
    }

    LOG.info("Captcha hasn't been solved by {}", userId);

    String key = key(chatId, userId);
    ActivePuzzle info = activePuzzles.get(key);
    info.getPuzzleThread().interrupt();
    Bot.kickUser(bot, query.getMessage().getChatId(), userId, PUZZLE_BAN_TIME);
    removeMessages(bot, chatId, info);
    activePuzzles.remove(key);

    return null;
  }

  @CallbackMethod(
      data = "solve",
      origin = CallbackOrigin.MESSAGE,
      locality = {Locality.GROUP, Locality.SUPERGROUP}
  )
  public BotApiMethod onSolve(AbsSender bot, CallbackQuery query, CallbackOrigin origin) {
    Integer userId = query.getFrom().getId();
    Long chatId = query.getMessage().getChatId();
    Integer messageId = query.getMessage().getMessageId();
    if (!hasAccess(userId, chatId, messageId)) {
      return accessDeniedMessage(query.getId());
    }

    if (puzzle.hasNext()) {
      try {
        EditMessageText newChallenge = (EditMessageText) puzzle.nextPuzzle(query);
        newChallenge
            .setChatId(chatId)
            .setMessageId(messageId)
            .enableMarkdown(true);
        bot.execute(newChallenge);
        return successMessage(query.getId());
      } catch (TelegramApiException e) {
        LOG.error("Cannot generate a new puzzle, complete it");
      }
    }

    LOG.info("Captcha has been solved by {}", userId);

    String key = key(chatId, userId);
    ActivePuzzle info = activePuzzles.get(key);
    info.getPuzzleThread().interrupt();
    removeMessages(bot, chatId, info);
    activePuzzles.remove(key);

    return Bot.unmuteUser(chatId, userId);
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
      reason = {LeaveReason.KICK, LeaveReason.SELF}
  )
  public BotApiMethod onUserLeft(AbsSender bot, Message message, LeaveReason reason) {
    User firstUser = message.getLeftChatMember();
    int userId = reason == LeaveReason.SELF ? message.getFrom().getId() : firstUser.getId();

    try {
      bot.execute(Bot.deleteMessage(message));
    } catch (TelegramApiException e) {
      LOG.warn("Cannot remove a message: {}. {}", userId, e.getMessage());
    }

    String key = key(message.getChatId(), userId);
    if (activePuzzles.containsKey(key)) {
      ActivePuzzle info = activePuzzles.get(key);
      info.getPuzzleThread().interrupt();
      removeMessages(bot, message.getChatId(), info);
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

  private BotApiMethod accessDeniedMessage(String queryId) {
    return Bot.createCallbackMessage(queryId, "It's Not Your Fight Anymore");
  }

  private BotApiMethod successMessage(String queryId) {
    return Bot.createCallbackMessage(queryId, "Well Done");
  }

  private void removeMessages(AbsSender bot, long chatId, ActivePuzzle puzzle) {
    try {
      bot.execute(Bot.deleteMessage(chatId, puzzle.getJoinMessageId()));
      bot.execute(Bot.deleteMessage(chatId, puzzle.getPuzzleMessageId()));
    } catch (TelegramApiException e) {
      LOG.warn("Cannot remove a message: {}. {}", puzzle, e.getMessage());
    }
  }

  private String key(Long chatId, Integer userId) {
    return chatId.toString() + userId.toString();
  }
}
