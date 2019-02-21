package com.github.snqlby.guardbot;

import com.github.snqlby.tgwebhook.AcceptTypes;
import com.github.snqlby.tgwebhook.Handlers;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {

  private static final Logger LOG = LoggerFactory.getLogger(Application.class);

  /**
   * Entry point of application.
   *
   * @param args passed args
   */
  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
    final Map<String, Object> handlers = context.getBeansWithAnnotation(AcceptTypes.class);
    handlers
        .values()
        .forEach(
            handler -> {
              Handlers.addHandler(handler);
              LOG.info("Loaded handler: {}", handler.getClass().getSimpleName());
            });
  }
}
