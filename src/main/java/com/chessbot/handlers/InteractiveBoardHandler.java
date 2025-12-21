package com.chessbot.handlers;

import com.chessbot.dto.GameResponse;
import com.chessbot.service.ApiClient;
import com.chessbot.service.GameSessionManager;
import com.chessbot.service.InteractiveBoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InteractiveBoardHandler {

    private final ApiClient apiClient;
    private final GameSessionManager sessionManager;
    private final InteractiveBoardService boardService;

    // Состояния выбора хода
    private final Map<Long, String> selectedSquare = new HashMap<>();

    /**
     * 🎮 СОЗДАНИЕ ИНТЕРАКТИВНОЙ ДОСКИ (БЕЗОПАСНЫЙ ВАРИАНТ)
     */
    public BoardMessage createInteractiveBoardMessage(Long chatId) {
        try {
            String gameId = sessionManager.getCurrentGameId(chatId);
            if (gameId == null) {
                return new BoardMessage(
                        "❌ *У вас нет активной игры!*\n\n" +
                                "Создайте новую: `/newgame`",
                        null
                );
            }

            // Получаем состояние игры
            GameResponse response = apiClient.getGameState(gameId, chatId);
            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                return new BoardMessage(
                        "❌ *Ошибка получения доски!*\n\n" +
                                "Игра может быть завершена.",
                        null
                );
            }

            // Получаем цвет игрока
            String playerColor = "WHITE";
            GameSessionManager.GameSession session = sessionManager.getSession(chatId);
            if (session != null && session.getPlayerColor() != null) {
                playerColor = session.getPlayerColor();
            }

            // Получаем возможные ходы (если API поддерживает)
            List<String> legalMoves = new ArrayList<>();
            try {
                legalMoves = apiClient.getLegalMoves(gameId, chatId);
            } catch (Exception e) {
                log.warn("Не удалось получить возможные ходы: {}", e.getMessage());
            }

            // Создаем интерактивную доску
            InteractiveBoardService.InteractiveBoard interactiveBoard =
                    boardService.createInteractiveBoard(
                            response.getBoard(),
                            playerColor,
                            legalMoves
                    );

            // Форматируем сообщение
            String message = formatBoardMessage(response, interactiveBoard.getBoardText());

            return new BoardMessage(message, interactiveBoard.getKeyboard());

        } catch (Exception e) {
            log.error("Ошибка создания интерактивной доски: {}", e.getMessage(), e);

            // Возвращаем упрощенную доску без клавиатуры
            return new BoardMessage(
                    "🎮 *Шахматная доска*\n\n" +
                            "⚠️ *Интерактивная доска временно недоступна*\n\n" +
                            "📋 *Что вы можете сделать:*\n" +
                            "• Сделать ход: `/move e2-e4`\n" +
                            "• Посмотреть состояние: `/status`\n" +
                            "• Получить помощь: `/help`",
                    null
            );
        }
    }

    /**
     * 🔘 ОБРАБОТКА НАЖАТИЯ НА КЛЕТКУ (БЕЗОПАСНЫЙ ВАРИАНТ)
     */
    public BoardMessage handleSquareClick(Long chatId, String square, String userName) {
        try {
            String gameId = sessionManager.getCurrentGameId(chatId);
            if (gameId == null) {
                return new BoardMessage("❌ *Игра не найдена!*", null);
            }

            // Пока просто возвращаем сообщение, что фигура выбрана
            // В будущем здесь будет логика выбора хода
            return new BoardMessage(
                    "🎯 *Выбрана фигура на клетке " + square.toUpperCase() + "*\n\n" +
                            "📋 *Для выполнения хода используйте команду:*\n" +
                            "`/move " + square + "-[целевая клетка]`\n\n" +
                            "📖 *Пример:* `/move " + square + "-e4`",
                    null
            );

        } catch (Exception e) {
            log.error("Ошибка обработки клика: {}", e.getMessage(), e);
            selectedSquare.remove(chatId);
            return new BoardMessage("❌ *Ошибка:* " + e.getMessage(), null);
        }
    }

    /**
     * 🔄 ОБРАБОТКА КНОПКИ ОБНОВЛЕНИЯ
     */
    public BoardMessage handleRefresh(Long chatId) {
        selectedSquare.remove(chatId);
        return createInteractiveBoardMessage(chatId);
    }

    /**
     * 📋 ФОРМАТИРОВАНИЕ СООБЩЕНИЯ С ДОСКОЙ
     */
    private String formatBoardMessage(GameResponse response, String boardText) {
        StringBuilder sb = new StringBuilder();

        sb.append("♟️ *Шахматная доска*\n\n");

        if (response.getMessage() != null && !response.getMessage().isEmpty()) {
            sb.append("💬 ").append(response.getMessage()).append("\n\n");
        }

        sb.append("🆔 ID: `").append(response.getGameId()).append("`\n");
        sb.append("📊 Статус: ").append(response.getStatus()).append("\n");
        sb.append("🎮 Очередь: ").append(response.getCurrentTurn()).append("\n\n");

        sb.append(boardText).append("\n");

        // Информация об игроках
        if (response.getWhitePlayer() != null && response.getBlackPlayer() != null) {
            sb.append("\n👥 *Игроки:*\n");
            sb.append("⚪ *Белые:* ").append(response.getWhitePlayer().getName());
            if (response.getWhitePlayer().getRating() != null) {
                sb.append(" ⭐").append(response.getWhitePlayer().getRating());
            }
            sb.append("\n");

            sb.append("⚫ *Черные:* ").append(response.getBlackPlayer().getName());
            if (response.getBlackPlayer().getRating() != null) {
                sb.append(" ⭐").append(response.getBlackPlayer().getRating());
            }
            sb.append("\n");
        }

        sb.append("\n🎯 *Кликните на фигуру чтобы выбрать ее для хода!*");

        return sb.toString();
    }

    /**
     * 📨 DTO ДЛЯ ВОЗВРАТА СООБЩЕНИЯ
     */
    public static class BoardMessage {
        private final String text;
        private final InlineKeyboardMarkup keyboard;

        public BoardMessage(String text, InlineKeyboardMarkup keyboard) {
            this.text = text;
            this.keyboard = keyboard;
        }

        public String getText() {
            return text;
        }

        public InlineKeyboardMarkup getKeyboard() {
            return keyboard;
        }
    }
}