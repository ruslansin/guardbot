package com.github.snqlby.guardbot.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.github.snqlby.guardbot.data.ParameterData;
import com.github.snqlby.guardbot.puzzle.MinesweeperPuzzle;
import com.github.snqlby.guardbot.puzzle.Puzzle;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
public class PuzzleSessionService {

  private final Map<String, ActivePuzzle> activePuzzles = new ConcurrentHashMap<>();

  private final ParameterService parameterService;

  public PuzzleSessionService(ParameterService parameterService) {
    this.parameterService = parameterService;
  }

  public ActivePuzzle create(Message message) {
    long chatId = message.getChatId();
    int userId = message.getFrom().getId();
    int joinMessageId = message.getMessageId();
    String key = key(chatId, userId);
    String solveEmoji = parameterService.findParameterOrDefault(ParameterData.MODULE_MINESWEEPER_SOLVE_SYMBOL, chatId, "\uD83C\uDF00");
    String banEmoji = parameterService.findParameterOrDefault(ParameterData.MODULE_MINESWEEPER_SOLVE_SYMBOL, chatId, "\uD83D\uDCA3");
    Puzzle chatPuzzle = new MinesweeperPuzzle(4, solveEmoji, banEmoji);
    ActivePuzzle activePuzzle = new ActivePuzzle(null, userId, joinMessageId, null, chatPuzzle);
    activePuzzles.put(key, activePuzzle);
    return activePuzzle;
  }

  public ActivePuzzle find(Long chatId, Integer userId) {
    return activePuzzles.get(key(chatId, userId));
  }

  public Optional<Integer> findPuzzleOwner(Integer messageId) {
    return activePuzzles.values().stream()
        .filter(p -> p.getPuzzleMessageId().equals(messageId))
        .map(ActivePuzzle::getUserId).findFirst();
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

  public void start(Message message, Integer puzzleMessageId, Runnable runnable) {
    long chatId = message.getChatId();
    int userId = message.getFrom().getId();
    Thread puzzleThread = new Thread(runnable);
    ActivePuzzle activePuzzle = find(chatId, userId);
    activePuzzle.setPuzzleMessageId(puzzleMessageId);
    activePuzzle.setPuzzleThread(puzzleThread);
    puzzleThread.start();
  }

}
