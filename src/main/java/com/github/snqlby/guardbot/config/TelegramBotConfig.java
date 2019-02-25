package com.github.snqlby.guardbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration("guardbotConfig")
@ConfigurationProperties(prefix = "guardbot.telegram")
public class TelegramBotConfig {

  private String token;

  private String username;

  private String url;

  private String certificate;

  private boolean updateWebhook = true;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getCertificate() {
    return certificate;
  }

  public void setCertificate(String certificate) {
    this.certificate = certificate;
  }

  public boolean isUpdateWebhook() {
    return updateWebhook;
  }

  public void setUpdateWebhook(boolean updateWebhook) {
    this.updateWebhook = updateWebhook;
  }
}
