package com.github.snqlby.guardbot.handler;

import static com.github.snqlby.guardbot.util.Constants.PUZZLE_AUTOFAIL_TIME;
import static com.github.snqlby.guardbot.util.Constants.PUZZLE_BAN_TIME;
import static com.github.snqlby.guardbot.util.Constants.PUZZLE_CHALLENGE_TIME;

import com.github.snqlby.guardbot.data.ParameterData;
import com.github.snqlby.guardbot.puzzle.Puzzle;
import com.github.snqlby.guardbot.service.ActivePuzzle;
import com.github.snqlby.guardbot.service.AdminService;
import com.github.snqlby.guardbot.service.ParameterService;
import com.github.snqlby.guardbot.service.PuzzleSessionService;
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

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

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
public class JoinLeaveHandler {

  private static final Logger LOG = LoggerFactory.getLogger(JoinLeaveHandler.class);
  private final PuzzleSessionService puzzleSessionService;
  private final AdminService adminService;
  private final ParameterService parameterService;

  public JoinLeaveHandler(PuzzleSessionService puzzleSessionService, AdminService adminService, ParameterService parameterService) {
    this.puzzleSessionService = puzzleSessionService;
    this.adminService = adminService;
    this.parameterService = parameterService;
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
    Long chatId = message.getChatId();

    Boolean captchaModuleEnabled = parameterService.findParameterOrDefault(ParameterData.MODULE_CAPTCHA_ENABLED, chatId, true);
    Boolean joinDeleteModuleEnabled = parameterService.findParameterOrDefault(ParameterData.MODULE_DELETE_JOIN_MESSAGE_ENABLED, chatId, true);
    if (captchaModuleEnabled) {
      startCaptcha(bot, message);
    } else if (joinDeleteModuleEnabled) {
      // normally captcha module will remove join messages. in case the module is enabled, we will join delete messages
      try {
        bot.execute(Bot.deleteMessage(chatId, message.getMessageId()));
      } catch (TelegramApiException e) {
        LOG.error("Cannot delete a join message {} from {}", message.getMessageId(), chatId);
      }
    }

    return null;
  }

  private void startCaptcha(AbsSender bot, Message message) {
    User user = message.getFrom();
    Integer userId = user.getId();
    Long chatId = message.getChatId();
    Bot.muteUser(bot, chatId, userId);

    Integer joinMessageId = message.getMessageId();
    String chatLocale = parameterService.findParameterOrDefault(ParameterData.CHAT_LOCALE, chatId, "en");
    Map<String, Object> params = Map.of(
            "firstName", user.getFirstName(),
            "locale", chatLocale
    );
    ActivePuzzle activePuzzle = puzzleSessionService.create(message);
    Puzzle puzzle = activePuzzle.getPuzzle();
    SendMessage puzzleMessage = (SendMessage) puzzle.nextPuzzle(message, params);
    puzzleMessage
            .setChatId(chatId)
            .setReplyToMessageId(joinMessageId)
            .enableMarkdown(true)
            .disableNotification();
    Integer puzzleMessageId;
    try {
      puzzleMessageId = bot.execute(puzzleMessage).getMessageId();
      puzzleSessionService.start(message, puzzleMessageId, () -> {
        try {
          Thread.sleep(PUZZLE_CHALLENGE_TIME * 1000);
        } catch (InterruptedException e) {
          LOG.debug("Task has been interrupted: chatId {}, userId {}", chatId, userId);
          return;
        }

        if (puzzleSessionService.isAlive(chatId, userId)) {
          LOG.info("Puzzle was not solved by {}", userId);
          Bot.kickUser(bot, chatId, userId, PUZZLE_AUTOFAIL_TIME);
          removeMessages(bot, chatId, activePuzzle);
          puzzleSessionService.destroy(chatId, userId);
        }
      });
    } catch (TelegramApiException e) {
      LOG.error("Cannot send puzzle to user {}", userId);
    }
  }

  @CallbackMethod(
      data = "banme",
      origin = CallbackOrigin.MESSAGE,
      locality = {Locality.GROUP, Locality.SUPERGROUP}
  )
  public BotApiMethod onBanMe(AbsSender bot, CallbackQuery query, CallbackOrigin origin) {
    Integer userId = query.getFrom().getId();
    Message message = query.getMessage();
    Long chatId = message.getChatId();
    Message originalMessage = message.getReplyToMessage();
    boolean isUserAdmin = adminService.isAdmin(chatId, userId);
    if (!hasAccess(userId, chatId, message.getMessageId()) && !isUserAdmin) {
      String chatLocale = parameterService.findParameterOrDefault(ParameterData.CHAT_LOCALE, chatId, "en");
      return accessDeniedMessage(query.getId(), chatLocale);
    }

    User originalUser = originalMessage.getFrom();
    Integer originalUserId = originalUser.getId();
    LOG.info("Captcha hasn't been solved for {}", originalUserId);

    ActivePuzzle info = puzzleSessionService.find(chatId, originalUserId);
    Bot.kickUser(bot, message.getChatId(), originalUserId, PUZZLE_BAN_TIME);
    removeMessages(bot, chatId, info);
    puzzleSessionService.destroy(chatId, originalUserId);

    return null;
  }

  @CallbackMethod(
      data = "solve",
      origin = CallbackOrigin.MESSAGE,
      locality = {Locality.GROUP, Locality.SUPERGROUP}
  )
  public BotApiMethod onSolve(AbsSender bot, CallbackQuery query, CallbackOrigin origin) {
    Integer userId = query.getFrom().getId();
    Message message = query.getMessage();
    Long chatId = message.getChatId();
    Message originalMessage = message.getReplyToMessage();
    Integer messageId = message.getMessageId();
    boolean isUserAdmin = adminService.isAdmin(chatId, userId);
    if (!hasAccess(userId, chatId, messageId) && !isUserAdmin) {
      String chatLocale = parameterService.findParameterOrDefault(ParameterData.CHAT_LOCALE, chatId, "en");
      return accessDeniedMessage(query.getId(), chatLocale);
    }

    User originalUser = originalMessage.getFrom();
    Integer originalUserId = originalUser.getId();

    ActivePuzzle activePuzzle = puzzleSessionService.find(chatId, userId);
    Puzzle puzzle = activePuzzle.getPuzzle();
    if (puzzle.hasNext()) {
      try {
        String chatLocale = parameterService.findParameterOrDefault(ParameterData.CHAT_LOCALE, chatId, "en");
        Map<String, Object> params = Map.of(
                "firstName", originalUser.getFirstName(),
                "locale", chatLocale
        );
        EditMessageText newChallenge = (EditMessageText) puzzle.nextPuzzle(query, params);
        newChallenge
            .setChatId(chatId)
            .setMessageId(messageId)
            .enableMarkdown(true);
        bot.execute(newChallenge);
        return successMessage(query.getId(), chatLocale);
      } catch (TelegramApiException e) {
        LOG.warn("Cannot generate a new puzzle, complete it: {}", e.getMessage());
      }
    }

    LOG.info("Captcha has been solved for {}", originalUserId);

    removeMessages(bot, chatId, activePuzzle);
    puzzleSessionService.destroy(chatId, originalUserId);

    return Bot.unmuteUser(chatId, originalUserId);
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
    long chatId = message.getChatId();

    try {
      bot.execute(Bot.deleteMessage(message));
    } catch (TelegramApiException e) {
      LOG.warn("Cannot remove a message: {}. {}", userId, e.getMessage());
    }

    if (puzzleSessionService.isAlive(chatId, userId)) {
      ActivePuzzle info = puzzleSessionService.find(chatId, userId);
      removeMessages(bot, message.getChatId(), info);
      puzzleSessionService.destroy(chatId, userId);
    }

    return null;
  }

  private boolean hasAccess(Integer userId, Long chatId, Integer messageId) {
    if (!puzzleSessionService.isAlive(chatId, userId)) {
      return false;
    } else {
      ActivePuzzle puzzle = puzzleSessionService.find(chatId, userId);
      return puzzle.getPuzzleMessageId().equals(messageId);
    }
  }

  private BotApiMethod accessDeniedMessage(String queryId, String locale) {
    Locale chatLocale = new Locale(locale);
    ResourceBundle bundle = ResourceBundle.getBundle("strings", chatLocale);
    return Bot.createCallbackMessage(queryId, bundle.getString("puzzle.minesweeper.solve.denied"));
  }

  private BotApiMethod successMessage(String queryId, String locale) {
    Locale chatLocale = new Locale(locale);
    ResourceBundle bundle = ResourceBundle.getBundle("strings", chatLocale);
    return Bot.createCallbackMessage(queryId, bundle.getString("puzzle.minesweeper.solve.success"));
  }

  private void removeMessages(AbsSender bot, long chatId, ActivePuzzle puzzle) {
    try {
      bot.execute(Bot.deleteMessage(chatId, puzzle.getJoinMessageId()));
      bot.execute(Bot.deleteMessage(chatId, puzzle.getPuzzleMessageId()));
    } catch (TelegramApiException e) {
      LOG.warn("Cannot remove a message: {}. {}", puzzle, e.getMessage());
    }
  }
}
