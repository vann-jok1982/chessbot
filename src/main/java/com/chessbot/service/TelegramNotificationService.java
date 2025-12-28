package com.chessbot.service;

import com.chessbot.config.LongPollingBotConfig;
import com.chessbot.dto.GameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * 🚀 Сервис для отправки уведомлений через Telegram
 */
@Service
@Slf4j
//@RequiredArgsConstructor
public class TelegramNotificationService {


    private LongPollingBotConfig telegramBot;

    public TelegramNotificationService(@Lazy LongPollingBotConfig telegramBot) {
        this.telegramBot = telegramBot;
    }

    /**
     * Отправляет уведомление о ходе сопернику
     */
    public void sendMoveNotification(Long opponentChatId, GameResponse gameResponse, String moveNotation) {
        if (opponentChatId == null) {
            log.warn("⚠️ Не указан chatId соперника для уведомления");
            return;
        }

        try {
            String message = createMoveNotificationMessage(gameResponse, moveNotation);
            sendMessage(opponentChatId, message);

            log.info("✅ Уведомление о ходе отправлено в chatId={}", opponentChatId);

        } catch (Exception e) {
            log.error("❌ Ошибка отправки уведомления: {}", e.getMessage(), e);
        }
    }

    /**
     * Создает текст уведомления о ходе
     */
    private String createMoveNotificationMessage(GameResponse response, String moveNotation) {
        // Получаем имя игрока, который сделал ход
        String playerName = getPlayerName(response, moveNotation);

        return """
               ♟️ *СОПЕРНИК СДЕЛАЛ ХОД!*
               
               👤 *Игрок:* %s
               🎮 *Игра:* `%s`
               📝 *Ход:* `%s`
               
               📊 *Статус:* %s
               
               🎯 *Текущая доска:*
               ```
               %s
               ```
               
               🕐 *Ваша очередь!*
               Сделайте ход: `/move [ход]`
               
               🔍 *Посмотреть доску:* `/board`
               📋 *Возможные ходы:* `/moves`
               """.formatted(
                playerName,
                response.getGameId(),
                moveNotation,
                getStatusMessage(response.getStatus()),
                response.getBoard() != null ? response.getBoard() : "Доска недоступна"
        );
    }

    /**
     * Определяет имя игрока по цвету
     */
    private String getPlayerName(GameResponse response, String moveNotation) {
        // Если это ход белых
        if ("WHITE".equals(response.getCurrentTurn())) {
            // Сейчас ходят белые, значит только что ходили черные
            return response.getBlackPlayer() != null ?
                    response.getBlackPlayer().getName() : "Соперник";
        } else {
            // Сейчас ходят черные, значит только что ходили белые
            return response.getWhitePlayer() != null ?
                    response.getWhitePlayer().getName() : "Соперник";
        }
    }

    /**
     * Форматирует статус игры
     */
    private String getStatusMessage(String status) {
        if (status == null) return "Игра продолжается";

        switch (status.toUpperCase()) {
            case "CHECK": return "ШАХ!";
            case "CHECKMATE": return "МАТ!";
            case "STALEMATE": return "ПАТ!";
            case "DRAW": return "Ничья!";
            default: return "Игра продолжается";
        }
    }

    /**
     * Отправляет сообщение в Telegram
     */
    public void sendMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.enableHtml(true);
        message.setParseMode("HTML");

        telegramBot.execute(message);
    }

    /**
     * Отправляет простое текстовое сообщение
     */
    public void sendSimpleMessage(Long chatId, String text) {
        try {
            sendMessage(chatId, text);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения в chatId={}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Отправляет уведомление о начале игры
     */
    public void sendGameStartNotification(Long opponentChatId, String gameId, String opponentName) {
        String message = """
               🎮 *НОВАЯ ИГРА!*
               
               👤 *Соперник:* %s
               🆔 *ID игры:* `%s`
               
               🎯 *Игра началась!*
               Сделайте первый ход: `/move [ход]`
               
               📖 *Пример:* `/move e2-e4`
               """.formatted(opponentName, gameId);

        sendSimpleMessage(opponentChatId, message);
    }
}