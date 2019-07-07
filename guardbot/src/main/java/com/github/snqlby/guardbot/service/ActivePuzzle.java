package com.github.snqlby.guardbot.service;

public class ActivePuzzle {

  private final Thread puzzleThread;
  private Integer userId;
  private Integer joinMessageId;
  private Integer puzzleMessageId;

  public ActivePuzzle(Integer userId, Integer joinMessageId, Integer puzzleMessageId,
      Thread puzzleThread) {
    this.userId = userId;
    this.joinMessageId = joinMessageId;
    this.puzzleMessageId = puzzleMessageId;
    this.puzzleThread = puzzleThread;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
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
