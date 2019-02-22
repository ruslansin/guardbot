package com.github.snqlby.guardbot.service;

public class ActivePuzzle {

  private Integer joinMessageId;
  private Integer puzzleMessageId;
  private final Thread puzzleThread;

  public ActivePuzzle(Integer joinMessageId, Integer puzzleMessageId, Thread puzzleThread) {
    this.joinMessageId = joinMessageId;
    this.puzzleMessageId = puzzleMessageId;
    this.puzzleThread = puzzleThread;
  }

  public Integer getJoinMessageId() {
    return joinMessageId;
  }

  public void setJoinMessageId(Integer joinMessageId) {
    this.joinMessageId = joinMessageId;
  }

  public Integer getPuzzleMessageId() {
    return puzzleMessageId;
  }

  public void setPuzzleMessageId(Integer puzzleMessageId) {
    this.puzzleMessageId = puzzleMessageId;
  }

  public Thread getPuzzleThread() {
    return puzzleThread;
  }

  @Override
  public String toString() {
    return "ActivePuzzle{" +
        "joinMessageId=" + joinMessageId +
        ", puzzleMessageId=" + puzzleMessageId +
        '}';
  }
}
