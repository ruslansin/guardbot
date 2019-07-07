package com.github.snqlby.guardbot.service;

public interface AdminService {

  /**
   * Check if user is an admin.
   */
  boolean isAdmin(long chatId, long userId);

  /**
   * Check if the chat admins are present in the cache.
   */
  boolean isChatPresent(long chatId);

  /**
   * Attempt to reload admins for the chat.
   */
  boolean reload(long chatId);

}
