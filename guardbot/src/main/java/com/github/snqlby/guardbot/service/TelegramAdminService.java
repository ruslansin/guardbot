package com.github.snqlby.guardbot.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class TelegramAdminService implements AdminService {

  private static final Logger LOG = LoggerFactory.getLogger(TelegramAdminService.class);

  private TelegramBotService botService;
  private Map<Long, List<Long>> admins = new HashMap<>();

  public TelegramAdminService(TelegramBotService botService) {
    this.botService = botService;
  }

  @Override
  public boolean isAdmin(long chatId, long userId) {
    if (!isChatPresent(chatId)) {
      boolean success = reload(chatId);
      if (!success) {
        return false;
      }
    }

    return admins.get(chatId).contains(userId);
  }

  @Override
  public boolean isChatPresent(long chatId) {
    return admins.containsKey(chatId);
  }

  @Override
  public boolean reload(long chatId) {

    synchronized (this) {
      GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
      getChatAdministrators.setChatId(chatId);
      try {
        List<ChatMember> administrators = botService.execute(getChatAdministrators);
        admins.put(chatId, administrators.stream()
            .map(m -> m.getUser().getId().longValue())
            .collect(Collectors.toList()));
        return true;
      } catch (TelegramApiException e) {
        LOG.warn("Can't reload the admins for {}: {}", chatId, e.getMessage());
      }
    }

    return false;
  }
}
