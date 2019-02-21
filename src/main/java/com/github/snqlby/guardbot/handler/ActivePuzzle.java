package com.github.snqlby.guardbot.handler;

public class ActivePuzzle {
  private Integer joinMessageId;
  private Integer puzzleMessageId;
  private String firstName;

  public ActivePuzzle(Integer joinMessageId, Integer puzzleMessageId, String firstName) {
    this.joinMessageId = joinMessageId;
    this.puzzleMessageId = puzzleMessageId;
    this.firstName = firstName;
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

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @Override
  public String toString() {
    return "ActivePuzzle{" +
        "joinMessageId=" + joinMessageId +
        ", puzzleMessageId=" + puzzleMessageId +
        ", firstName='" + firstName + '\'' +
        '}';
  }
}
