package com.chessbot.config;

import com.chessbot.bot.BotFasade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public  class LongPollingBotConfig extends TelegramLongPollingBot {

    private final BotFasade botFasade;
    private static final Logger logger = LoggerFactory.getLogger(LongPollingBotConfig.class);

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public void onUpdateReceived(Update update) {
        logger.info("Received update: {}", update);

        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage sendMessage = botFasade.obrabotkaHandleUpdate(update);
            if (sendMessage != null) {
                try {
                    execute(sendMessage);
                    logger.info("Message sent successfully to chat: {}", sendMessage.getChatId());
                } catch (TelegramApiException e) {
                    logger.error("Error sending message: {}", e.getMessage(), e);
                    // Не бросаем RuntimeException, чтобы бот продолжал работать
                    // Можно добавить обработку ошибок (повторные попытки и т.д.)
                }
            }
        } else {
            logger.warn("Update doesn't contain text message: {}", update);
        }
    }



    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}