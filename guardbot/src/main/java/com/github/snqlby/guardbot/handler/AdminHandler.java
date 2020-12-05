package com.github.snqlby.guardbot.handler;

import com.github.snqlby.guardbot.data.ParameterData;
import com.github.snqlby.guardbot.service.AdminService;
import com.github.snqlby.guardbot.service.ParameterService;
import com.github.snqlby.tgwebhook.AcceptTypes;
import com.github.snqlby.tgwebhook.Locality;
import com.github.snqlby.tgwebhook.UpdateType;
import com.github.snqlby.tgwebhook.methods.CommandMethod;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

@AcceptTypes({UpdateType.MESSAGE})
@Component
public class AdminHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AdminHandler.class);
  private final AdminService adminService;
  private final ParameterService parameterService;

  public AdminHandler(AdminService adminService, ParameterService parameterService) {
    this.adminService = adminService;
    this.parameterService = parameterService;
  }

  /**
   * Reload admins for the chat.
   *
   * @param bot bot instance
   * @param message received message
   * @param args command args
   * @return null if user doesn't exists
   */
  @CommandMethod(
      locality = {Locality.GROUP, Locality.SUPERGROUP},
      command = "/reload"
  )
  public BotApiMethod onReload(AbsSender bot, Message message, List<String> args) {
    Long chatId = message.getChatId();
    LOG.info("Reload was requested by {} from {}", message.getFrom().getId(), chatId);

    if (!adminService.isChatPresent(chatId)
        || adminService.isAdmin(chatId, message.getFrom().getId())) {
      boolean success = adminService.reload(chatId);
      if (success) {
        return new SendMessage(chatId, "*Admins were reloaded*").enableMarkdown(true);
      }
    }
    return new SendMessage(chatId, "*Admins were not reloaded*").enableMarkdown(true);
  }

  /**
   * Update chat settings.
   *
   * /parameter chatId parameter value
   * @param bot bot instance
   * @param message received message
   * @param args command args
   * @return null if user doesn't exists
   */
  @CommandMethod(
          locality = {Locality.PRIVATE},
          command = "/parameter"
  )
  public BotApiMethod onParametersRequest(AbsSender bot, Message message, List<String> args) {
    if (args.size() < 3) {
      return null;
    }

    long chatId = Long.parseLong(args.get(0));
    String name = args.get(1);
    String value = args.get(2);

    LOG.info("Parameter update were requested by {}", message.getFrom().getId());

    if (adminService.isAdmin(chatId, message.getFrom().getId())) {
      ParameterData data = ParameterData.findByName(name);
      if (Objects.isNull(data)) {
        return new SendMessage(message.getChatId(),"*Unknown parameter*")
                .setReplyToMessageId(message.getMessageId())
                .enableMarkdown(true);
      }
      parameterService.createOrUpdate(data, chatId, value);
      return new SendMessage(message.getChatId(),"*Parameter was updated*")
              .setReplyToMessageId(message.getMessageId())
              .enableMarkdown(true);
    }
    return null;
  }
}
