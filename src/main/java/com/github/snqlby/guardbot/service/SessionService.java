package com.github.snqlby.guardbot.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
public class SessionService {

  private Map<String, ActivePuzzle> activePuzzles = new ConcurrentHashMap<>();

  public void create(Message message,
      Integer puzzleMessageId, Runnable runnable) {
    long chatId = message.getChatId();
    int userId = message.getFrom().getId();
    int joinMessageId = message.getMessageId();
    String key = key(chatId, userId);
    Thread puzzleThread = new Thread(runnable);
    activePuzzles.put(key, new ActivePuzzle(joinMessageId, puzzleMessageId, puzzleThread));
    puzzleThread.start();
  }

  public ActivePuzzle find(Long chatId, Integer userId) {
    return activePuzzles.get(key(chatId, userId));
  }

  public void remove(Long chatId, Integer userId) {
    activePuzzles.remove(key(chatId, userId));
  }

  public boolean isAlive(Long chatId, Integer userId) {
    return activePuzzles.containsKey(key(chatId, userId));
  }

  public void destroy(Long chatId, Integer userId) {
    ActivePuzzle puzzle = find(chatId, userId);
    if (puzzle == null) {
      return;
    }
    if (Thread.currentThread() != puzzle.getPuzzleThread()) {
      puzzle.getPuzzleThread().interrupt();
    }
    remove(chatId, userId);
  }

  private String key(Long chatId, Integer userId) {
    return chatId.toString() + userId.toString();
  }

}
