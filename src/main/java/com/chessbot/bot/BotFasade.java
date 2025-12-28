package com.chessbot.bot;

import com.chessbot.handlers.ChessCommandHandler;
import com.chessbot.service.KeyboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotFasade {

    private final ChessCommandHandler commandHandler;
    private final KeyboardService keyboardService;

    /**
     * 🎯 ГЛАВНЫЙ МЕТОД ОБРАБОТКИ ОБНОВЛЕНИЙ...
     */
    public SendMessage obrabotkaHandleUpdate(Update update) {
        log.info("📨 Обработка обновления: {}", update.getUpdateId());

        try {
            // Обработка callback-запросов (нажатие inline кнопок)
            if (update.hasCallbackQuery()) {
                return handleCallbackQuery(update);
            }

            // Обработка текстовых сообщений
            if (!update.hasMessage() || !update.getMessage().hasText()) {
                log.warn("❌ Обновление не содержит текстового сообщения");
                return null;
            }

            // Извлекаем данные из сообщения
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            String userName = update.getMessage().getFrom().getUserName();

            // Если у пользователя нет username, используем first name
            if (userName == null || userName.isEmpty()) {
                userName = update.getMessage().getFrom().getFirstName();
                if (userName == null) userName = "Игрок";
            }

            log.info("💬 Сообщение от @{} (chatId: {}): {}", userName, chatId, text);

            // Обрабатываем команду
            String responseText = commandHandler.handleCommand(chatId, text, userName);

            // Создаем SendMessage с ответом
            SendMessage message = createSendMessage(chatId, responseText);

            // Добавляем клавиатуру для стартового сообщения
            if (text.startsWith("/start")) {
                message.setReplyMarkup(keyboardService.createMainMenuKeyboard());
            }

            return message;

        } catch (Exception e) {
            log.error("❌ Ошибка обработки обновления: {}", e.getMessage(), e);
            return createErrorMessage(update);
        }
    }

    /**
     * 🔘 ОБРАБОТКА CALLBACK-ЗАПРОСОВ
     */
    private SendMessage handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        String callbackData = callbackQuery.getData();
        String userName = callbackQuery.getFrom().getUserName();

        if (userName == null || userName.isEmpty()) {
            userName = callbackQuery.getFrom().getFirstName();
            if (userName == null) userName = "Игрок";
        }

        log.info("🔘 Callback от @{} (chatId: {}): {}", userName, chatId, callbackData);

        try {
            // Обрабатываем callback как обычную команду
            String responseText = commandHandler.handleCommand(chatId, callbackData, userName);
            SendMessage message = createSendMessage(chatId, responseText);

            // Удаляем inline клавиатуру после нажатия
            message.setReplyToMessageId(callbackQuery.getMessage().getMessageId());

            return message;

        } catch (Exception e) {
            log.error("Ошибка обработки callback: {}", e.getMessage(), e);
            return createErrorMessage(update);
        }
    }

    /**
     * 📤 СОЗДАНИЕ ОБЪЕКТА ДЛЯ ОТПРАВКИ СООБЩЕНИЯ
     */
    private SendMessage createSendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.enableHtml(true);
        message.setParseMode("HTML");

        log.debug("📤 Подготовлено сообщение для chatId: {}", chatId);
        return message;
    }

    /**
     * ❌ СОЗДАНИЕ СООБЩЕНИЯ ОБ ОШИБКЕ
     */
    private SendMessage createErrorMessage(Update update) {
        Long chatId = update.hasCallbackQuery() ?
                update.getCallbackQuery().getMessage().getChatId() :
                update.getMessage().getChatId();

        String errorMessage = """
                ❌ <b>Произошла ошибка</b>
                
                Пожалуйста, попробуйте еще раз.
                """;

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(errorMessage);
        message.enableHtml(true);
        message.setParseMode("HTML");

        return message;
    }
}