package com.github.snqlby.guardbot.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class MinesweeperPuzzle implements Puzzle {

  private final double complexity;

  private final String solveEmoji;

  private final String banEmoji;

  public MinesweeperPuzzle(double complexity, String solveEmoji, String banEmoji) {
    this.complexity = complexity;
    this.solveEmoji = solveEmoji;
    this.banEmoji = banEmoji;
  }

  @Override
  public boolean hasNext() {
    return randomMinMax(1, 10) > 6;
  }

  @Override
  public BotApiMethod<?> nextPuzzle(BotApiObject botApiObject, Map<String, Object> params) {
    if (botApiObject instanceof Message) {
      return createPuzzleButton(params);
    } else {
      return updatePuzzleButton(params);
    }
  }

  private SendMessage createPuzzleButton(Map<String, Object> params) {
    return new SendMessage()
        .setText(generatePuzzleMessage(params))
        .setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(generatePuzzle((int) complexity)));
  }

  private EditMessageText updatePuzzleButton(Map<String, Object> params) {
    return new EditMessageText()
        .setText(generatePuzzleMessage(params))
        .setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(generatePuzzle((int) complexity)));
  }

  private String generatePuzzleMessage(Map<String, Object> params) {
    Locale locale = new Locale((String) params.get("locale"));
    ResourceBundle bundle = ResourceBundle.getBundle("strings", locale);
    return String
        .format(bundle.getString("puzzle.minesweeper.hello.template"),
            params.get("firstName"), solveEmoji);
  }

  private List<List<InlineKeyboardButton>> generatePuzzle(int size) {
    int xSolve = randomMinMax(0, size - 1);
    int ySolve = randomMinMax(0, size - 1);
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    for (int x = 0; x < size; x++) {
      List<InlineKeyboardButton> row = new ArrayList<>();
      for (int y = 0; y < size; y++) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        String buttonIdentifier;
        String buttonText;
        if (x == xSolve && y == ySolve) {
          buttonText = solveEmoji;
          buttonIdentifier = "solve";
        } else {
          buttonText = banEmoji;
          buttonIdentifier = "banme";
        }
        button.setText(buttonText).setCallbackData(buttonIdentifier);
        row.add(button);
      }
      keyboard.add(row);
    }
    return keyboard;
  }

  private int randomMinMax(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }
}
