package com.github.snqlby.guardbot.handler;

public class ActivePuzzle {
  private Integer joinMessageId;
  private Integer puzzleMessageId;

  public ActivePuzzle(Integer joinMessageId, Integer puzzleMessageId) {
    this.joinMessageId = joinMessageId;
    this.puzzleMessageId = puzzleMessageId;
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

  @Override
  public String toString() {
    return "ActivePuzzle{" +
        "joinMessageId=" + joinMessageId +
        ", puzzleMessageId=" + puzzleMessageId +
        '}';
  }
}
