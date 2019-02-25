package com.github.snqlby.guardbot.service;


import static com.github.snqlby.guardbot.util.Constants.ADMIN_ID;

import com.github.snqlby.guardbot.config.TelegramBotConfig;
import com.github.snqlby.tgwebhook.RequestResolver;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.WebhookInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

@Service("guardbotService")
public class TelegramBotService extends TelegramWebhookBot {

  private static final Logger LOG = LoggerFactory.getLogger(TelegramBotService.class);

  private RequestResolver resolver;

  private TelegramBotConfig botConfig;

  /**
   * Process requests from Telegram.
   *
   * @param botConfig config instance
   * @throws TelegramApiException if webhook cannot be updated
   */
  public TelegramBotService(@Qualifier("guardbotConfig") TelegramBotConfig botConfig)
      throws TelegramApiException {
    this.botConfig = botConfig;

    this.resolver = new RequestResolver("guardbot", this);

    if (botConfig.isUpdateWebhook()) {
      updateWebhook(botConfig);
    }

    notifyAdmin();
  }

  private void notifyAdmin() {
    try {
      StringJoiner joiner = new StringJoiner("\n");
      joiner.add("Bot was started!");
      joiner.add(String.format("*Username: *`%s`", botConfig.getUsername()));
      if (botConfig.isUpdateWebhook()) {
        joiner.add(String.format("*Webhook url: *`%s`", botConfig.getUrl()));
      }
      execute(new SendMessage((long) ADMIN_ID, joiner.toString()).enableMarkdown(true));
    } catch (TelegramApiException e) {
      LOG.error("Cannot notify the admin with ID{}", ADMIN_ID);
    }
  }

  private void updateWebhook(TelegramBotConfig botConfig) throws TelegramApiException {
    Objects.requireNonNull(botConfig.getUsername(), "telegram.username cannot be null");
    Objects.requireNonNull(botConfig.getToken(), "telegram.token cannot be null");
    Objects.requireNonNull(botConfig.getUrl(), "telegram.url cannot be null");

    LOG.info("Loading webhook info...");
    WebhookInfo info = getWebhookInfo();
    LOG.info("Checking current webhook state...");
    String currentUrl = info.getUrl();
    String configUrl = makeWebhookUrl(botConfig.getUrl(), botConfig.getToken());
    Optional<String> certificateUrl = Optional.ofNullable(botConfig.getCertificate());

    if (currentUrl == null || currentUrl.isEmpty() || !currentUrl.equals(configUrl)) {
      LOG.info("Updating Telegram Webhook from {} to {} ...", currentUrl, configUrl);

      try {
        setWebhook(configUrl, certificateUrl.orElse(""));
      } catch (TelegramApiRequestException e) {
        LOG.warn("Cannot update Webhook: {}", e.getApiResponse());
      }
    } else {
      LOG.info("Webhook is up-to-date");
    }
  }

  private String makeWebhookUrl(String url, String token) {
    StringBuilder builder = new StringBuilder();
    String delimiter = url.endsWith("/") ? "" : "/";
    builder.append(url).append(delimiter).append(token);
    return builder.toString();
  }

  @Override
  public BotApiMethod onWebhookUpdateReceived(Update update) {
    Objects.requireNonNull(update, "Update cannot be null!");
    return resolver.handleRequest(update);
  }

  @Override
  public String getBotUsername() {
    return botConfig.getUsername();
  }

  @Override
  public String getBotToken() {
    return botConfig.getToken();
  }

  @Override
  public String getBotPath() {
    return botConfig.getUrl();
  }
}
