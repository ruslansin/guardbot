package com.github.snqlby.guardbot.service;

import com.github.snqlby.guardbot.puzzle.Puzzle;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ActivePuzzle {

  private Thread puzzleThread;
  private Integer userId;
  private Integer joinMessageId;
  private Integer puzzleMessageId;
  private Puzzle puzzle;
}
