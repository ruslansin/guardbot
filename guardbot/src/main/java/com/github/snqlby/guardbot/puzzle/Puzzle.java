package com.github.snqlby.guardbot.puzzle;

import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;

import java.util.Map;

public interface Puzzle {
  void setComplexity(double complexity);
  double getComplexity();
  boolean hasNext();
  BotApiMethod<?> nextPuzzle(BotApiObject botApiObject, Map<String, Object> params);
}
