package com.github.snqlby.guardbot.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Component
public class MinesweeperPuzzle implements Puzzle {

  private double complexity;

  @Override
  public double getComplexity() {
    return complexity;
  }

  @Override
  public void setComplexity(double complexity) {
    this.complexity = complexity;
  }

  @Override
  public boolean hasNext() {
    return randomMinMax(1, 10) > 6;
  }

  @Override
  public BotApiMethod<?> nextPuzzle(BotApiObject botApiObject) {
    if (botApiObject instanceof Message) {
      Message message = (Message) botApiObject;
      String firstName = message.getFrom().getFirstName();
      return createPuzzleButton(firstName);
    } else {
      CallbackQuery query = (CallbackQuery) botApiObject;
      String firstName = query.getFrom().getFirstName();
      return updatePuzzleButton(firstName);
    }
  }

  private SendMessage createPuzzleButton(String firstName) {
    return new SendMessage()
        .setText(generatePuzzleMessage(firstName))
        .setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(generatePuzzle((int) complexity)));
  }

  private EditMessageText updatePuzzleButton(String firstName) {
    return new EditMessageText()
        .setText(generatePuzzleMessage(firstName))
        .setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(generatePuzzle((int) complexity)));
  }

  private String generatePuzzleMessage(String firstName) {
    return String
        .format("Hello, %s. Let us make sure you are not a bot. **Find the Portal (\uD83C\uDF00)**",
            firstName);
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

  private int randomMinMax(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }
}
