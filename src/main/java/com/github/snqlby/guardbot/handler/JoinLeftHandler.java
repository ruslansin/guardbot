package com.github.snqlby.guardbot.handler;

import static com.github.snqlby.guardbot.util.Constants.WORLD_GROUP_ID;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@AcceptTypes({UpdateType.MESSAGE, UpdateType.CALLBACK_QUERY})
@Component
public class JoinLeftHandler {

  private static final Logger LOG = LoggerFactory.getLogger(JoinLeftHandler.class);
  private Map<Integer, ActivePuzzle> activePuzzles = new ConcurrentHashMap<>();

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
    muteUser(bot, userId);

    Integer joinMessageId = message.getMessageId();
    SendMessage puzzleMessage = generatePuzzleMessage(user.getFirstName(), joinMessageId);
    Integer puzzleMessageId;
    try {
      puzzleMessageId = bot.execute(puzzleMessage).getMessageId();
      activePuzzles.put(userId, new ActivePuzzle(joinMessageId, puzzleMessageId, user.getFirstName()));
    } catch (TelegramApiException e) {
      LOG.error("Cannot send puzzle to user {}", userId);
    }
    return null;
  }

  private SendMessage generatePuzzleMessage(String firstName, Integer messageId) {
    String replyMessage = String
        .format("Hello, %s. Let us make sure you are not a bot. **Find the Portal (\uD83C\uDF00)**",
            firstName);
    return new SendMessage(WORLD_GROUP_ID, replyMessage)
        .setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(generatePuzzle(randomMinMax(3, 6))))
        .setReplyToMessageId(messageId).enableMarkdown(true);
  }

  private int randomMinMax(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  private List<List<InlineKeyboardButton>> generatePuzzle(int size) {
    int xExit = randomMinMax(0, size);
    int yExit = randomMinMax(0, size);
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    for (int x = 0; x < size; x++) {
      List<InlineKeyboardButton> row = new ArrayList<>();
      for (int y = 0; y < size; y++) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        String buttonIdentifier;
        String buttonText;
        if (x == xExit && y == yExit) {
          buttonText = "\uD83C\uDF00";
          buttonIdentifier = "solve";
        } else {
          buttonText = "💣";
          buttonIdentifier = "banme";
        }
        button.setText(buttonText).setCallbackData(buttonIdentifier);
        row.add(button);
      }
      keyboard.add(row);
    }
    return keyboard;
  }

  private int findExitPosition(int bound, Random random) {
    return random.nextInt(bound);
  }

  @CallbackMethod(data = "banme", origin = CallbackOrigin.MESSAGE, locality = Locality.SUPERGROUP)
  public BotApiMethod onBanMe(AbsSender bot, CallbackQuery query, CallbackOrigin origin) {
    Integer userId = query.getFrom().getId();
    if (!activePuzzles.containsKey(userId)) {
      return accessDeniedMessage(userId.longValue());
    } else {
      ActivePuzzle puzzle = activePuzzles.get(userId);
      if (!puzzle.getPuzzleMessageId().equals(query.getMessage().getMessageId())) {
        return accessDeniedMessage(userId.longValue());
      }
    }

    kickUser(bot, userId);
    removeMessages(bot, activePuzzles.get(userId));
    activePuzzles.remove(userId);

    return null;
  }

  private BotApiMethod accessDeniedMessage(Long userId) {
    return new SendMessage(userId, "It's Not Your Fight any More")
        .enableNotification();
  }

  @CallbackMethod(data = "solve", origin = CallbackOrigin.MESSAGE, locality = Locality.SUPERGROUP)
  public BotApiMethod onSolve(AbsSender bot, CallbackQuery query, CallbackOrigin origin) {
    Integer userId = query.getFrom().getId();
    if (!hasAccess(userId, query.getMessage().getMessageId())) {
      return accessDeniedMessage(userId.longValue());
    }

    if (randomMinMax(1, 4) > 1) {
      ActivePuzzle puzzle = activePuzzles.get(userId);
      return generatePuzzleMessage(puzzle.getFirstName(), puzzle.getPuzzleMessageId());
    }

    removeMessages(bot, activePuzzles.get(userId));
    activePuzzles.remove(userId);

    return unmuteUser(userId);
  }

  private boolean hasAccess(Integer userId, Integer messageId) {
    if (!activePuzzles.containsKey(userId)) {
      return false;
    } else {
      ActivePuzzle puzzle = activePuzzles.get(userId);
      return puzzle.getPuzzleMessageId().equals(messageId);
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

    if (activePuzzles.containsKey(userId)) {
      removeMessages(bot, activePuzzles.get(userId));
      activePuzzles.remove(userId);
    }

    return null;
  }

  private void removeMessages(AbsSender bot, ActivePuzzle puzzle) {
    try {
      bot.execute(deleteMessage(WORLD_GROUP_ID, puzzle.getJoinMessageId()));
      bot.execute(deleteMessage(WORLD_GROUP_ID, puzzle.getPuzzleMessageId()));
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

  private boolean kickUser(AbsSender bot, int userId) {
    final int banDuration = 3600;
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
