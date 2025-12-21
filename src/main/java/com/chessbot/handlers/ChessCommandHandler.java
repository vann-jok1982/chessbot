package com.chessbot.handlers;

import com.chessbot.dto.GameResponse;
import com.chessbot.service.ApiClient;
import com.chessbot.service.GameSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ♟️ Основной обработчик команд шахматного бота
 * Отвечает за обработку всех пользовательских команд
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChessCommandHandler {

    private final ApiClient apiClient;
    private final GameSessionManager sessionManager;

    /**
     * 🎯 ГЛАВНЫЙ МЕТОД ОБРАБОТКИ КОМАНД
     */
    public String handleCommand(long chatId, String text, String userName) {
        log.info("Обработка команды: chatId={}, text='{}', user='{}'", chatId, text, userName);

        // Очищаем команду от лишних пробелов
        text = text.trim().toLowerCase();

        // Обработка команд
        if (text.startsWith("/start")) {
            return handleStart(chatId, userName);
        } else if (text.startsWith("/help")) {
            return handleHelp();
        } else if (text.startsWith("/newgame")) {
            return handleNewGame(chatId, userName);
        } else if (text.startsWith("/listgames")) {
            return handleListGames();
        } else if (text.startsWith("/joingame")) {
            return handleJoinGame(chatId, text, userName);
        } else if (text.startsWith("/move")) {
            return handleMove(chatId, text, userName);
        } else if (text.startsWith("/board")) {
            return handleBoard(chatId);
        } else if (text.startsWith("/status")) {
            return handleStatus();
        } else if (text.startsWith("/resign")) {
            return handleResign(chatId);
        } else if (text.startsWith("/moves")) {
            return handleLegalMoves(chatId);
        } else if (text.startsWith("/draw")) {
            return handleDraw(chatId, text, userName);

        } else if (text.startsWith("/resign")) {
            return handleResign(chatId);

        } else {
            return handleUnknownCommand(text);
        }
    }

    /**
     * 🏁 КОМАНДА /START
     */
    private String handleStart(long chatId, String userName) {
        return """
               ♟️ *Привет, %s! Я шахматный бот.*
               
               Я помогу тебе играть в шахматы с друзьями!
               
               🎮 *Основные команды:*
               /newgame - Создать новую игру
               /listgames - Список игр, ожидающих игроков
               /joingame [ID] - Присоединиться к игре
               /move [ход] - Сделать ход
               /board - Показать доску
               /moves - Показать возможные ходы
               /status - Статус сервера
               /help - Помощь
               
               🚀 *Начни игру:* /newgame
               """.formatted(userName);
    }

    /**
     * ❓ КОМАНДА /HELP
     */
    private String handleHelp() {
        return """
           📚 *Помощь по шахматному боту*
           
           🎮 *Основные команды:*
           • `/start` - Начало работы
           • `/help` - Эта справка
           
           🆕 *Создание и поиск игр:*
           • `/newgame` - Создать новую игру
           • `/listgames` - Список игр ожидающих игроков
           • `/joingame [ID]` - Присоединиться к игре
           
           ♟️ *Игровой процесс:*
           • `/move [ход]` - Сделать ход (e2-e4)
           • `/board` - Показать текущую доску
           • `/moves` - Показать возможные ходы
           
           🤝 *Завершение игры:*
           • `/draw` - Предложить ничью
           • `/draw accept` - Принять ничью
           • `/draw decline` - Отклонить ничью
           • `/resign` - Сдаться
           
           ⚙️ *Системные команды:*
           • `/status` - Статус сервера
           
           📖 *Пример игры:*
           1. Игрок 1: `/newgame`
           2. Игрок 2: `/joingame ABC123`
           3. Игрок 1: `/move e2-e4`
           4. Игрок 2: `/move e7-e5`
           5. И т.д...
           
           🐛 *Проблемы?* Используйте `/status`
           """;
    }

    /**
     * 🆕 КОМАНДА /NEWGAME
     */
    private String handleNewGame(long chatId, String userName) {
        try {
            // Проверяем, нет ли уже активной игры
            if (sessionManager.hasActiveGame(chatId)) {
                String currentGameId = sessionManager.getCurrentGameId(chatId);
                return """
                       ⚠️ *У вас уже есть активная игра!*
                       
                       🆔 Текущая игра: `%s`
                       
                       Чтобы создать новую игру, сначала завершите текущую:
                       • Закончите игру
                       • Или используйте `/resign`
                       """.formatted(currentGameId);
            }

            // Создаем новую игру через API
            log.info("Создание новой игры для chatId={}, user={}", chatId, userName);
            GameResponse response = apiClient.createGame(chatId, userName);

            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                return "❌ *Ошибка создания игры!*\n\n" +
                        (response != null ? response.getMessage() : "Сервер не отвечает");
            }

            // Сохраняем сессию
            sessionManager.createSession(response.getGameId(), chatId, chatId);

            return """
                   🎉 *Игра создана!*
                   
                   🆔 *ID игры:* `%s`
                   🎮 *Создатель:* %s
                   📊 *Статус:* %s
                   
                   📋 *Что делать дальше:*
                   1. Отправьте этот ID другу: `%s`
                   2. Друг отправляет: `/joingame %s`
                   3. Вы начинаете игру первым!
                   
                   ⏳ *Игра ждет второго игрока...*
                   """.formatted(
                    response.getGameId(),
                    userName,
                    response.getStatus(),
                    response.getGameId(),
                    response.getGameId()
            );

        } catch (Exception e) {
            log.error("Ошибка создания игры: {}", e.getMessage(), e);
            return "❌ *Ошибка создания игры:* " + e.getMessage();
        }
    }

    /**
     * 📋 КОМАНДА /LISTGAMES
     */
    private String handleListGames() {
        try {
            List<com.chessbot.dto.GameInfoResponse> games = apiClient.getWaitingGames();

            if (games.isEmpty()) {
                return """
                       🤷 *Нет игр, ожидающих игроков*
                       
                       Хотите сыграть? Создайте новую игру:
                       `/newgame`
                       """;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📋 *Игры, ожидающие игроков:*\n\n");

            for (com.chessbot.dto.GameInfoResponse game : games) {
                sb.append("🎮 *Игра ID:* `").append(game.getGameId()).append("`\n");
                sb.append("   👤 *Создатель:* ").append(game.getWhitePlayerName()).append("\n");
                sb.append("   🕐 *Создана:* ").append(game.getCreatedAt()).append("\n");
                sb.append("   🎯 *Присоединиться:* `/joingame ").append(game.getGameId()).append("`\n\n");
            }

            sb.append("🎯 *Выберите игру и присоединяйтесь!*");
            return sb.toString();

        } catch (Exception e) {
            log.error("Ошибка получения списка игр: {}", e.getMessage(), e);
            return "❌ *Ошибка получения списка игр:* " + e.getMessage();
        }
    }

    /**
     * 🤝 КОМАНДА /JOINGAME
     */
    private String handleJoinGame(long chatId, String text, String userName) {
        try {
            // Извлекаем ID игры из команды
            String[] parts = text.split("\\s+");
            if (parts.length < 2) {
                return """
                       ❌ *Не указан ID игры!*
                       
                       Использование: `/joingame [ID]`
                       Пример: `/joingame ABC123`
                       
                       Посмотреть доступные игры: `/listgames`
                       """;
            }

            String gameId = parts[1].trim();

            // Проверяем, нет ли уже активной игры
            if (sessionManager.hasActiveGame(chatId)) {
                String currentGameId = sessionManager.getCurrentGameId(chatId);
                return """
                       ⚠️ *У вас уже есть активная игра!*
                       
                       🆔 Текущая игра: `%s`
                       
                       Нельзя играть в две игры одновременно!
                       """.formatted(currentGameId);
            }

            // Присоединяемся к игре через API
            log.info("Присоединение к игре {} пользователем chatId={}", gameId, chatId);
            GameResponse response = apiClient.joinGame(gameId, chatId, userName);

            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                return "❌ *Ошибка присоединения к игре!*\n\n" +
                        (response != null ? response.getMessage() : "Игра не найдена");
            }

            // Сохраняем сессию
            sessionManager.createSession(gameId, chatId, chatId);

            // Обновляем информацию о цвете
            String playerColor = "WHITE";
            if (response.getWhitePlayer() != null && response.getWhitePlayer().getId() != null) {
                playerColor = response.getWhitePlayer().getId().equals(chatId) ? "WHITE" : "BLACK";
            }

            sessionManager.updateSession(chatId, playerColor, response.getStatus());

            return """
                   ✅ *Вы успешно присоединились к игре!*
                   
                   🆔 *ID игры:* `%s`
                   ♟️ *Ваш цвет:* %s
                   📊 *Статус:* %s
                   
                   🎮 *Текущая доска:*
                   ```
                   %s
                   ```
                   
                   %s
                   
                   🎯 *Сделать ход:* `/move [ход]`
                   Пример: `/move e2-e4`
                   """.formatted(
                    gameId,
                    playerColor,
                    response.getStatus(),
                    response.getBoard() != null ? response.getBoard() : "Доска не доступна",
                    getTurnMessage(response)
            );

        } catch (Exception e) {
            log.error("Ошибка присоединения к игре: {}", e.getMessage(), e);
            return "❌ *Ошибка присоединения:* " + e.getMessage();
        }
    }

    /**
     * ♟️ КОМАНДА /MOVE
     */
    private String handleMove(long chatId, String text, String userName) {
        try {
            // Проверяем, есть ли активная игра
            String gameId = sessionManager.getCurrentGameId(chatId);
            if (gameId == null) {
                return """
                       ❌ *У вас нет активной игры!*
                       
                       Чтобы начать игру:
                       1. Создайте новую: `/newgame`
                       2. Или присоединитесь: `/joingame [ID]`
                       """;
            }

            // Извлекаем ход из команды
            String[] parts = text.split("\\s+", 2);
            if (parts.length < 2) {
                return """
                       ❌ *Не указан ход!*
                       
                       Использование: `/move [ход]`
                       Примеры:
                       • `/move e2-e4`
                       • `/move g1-f3`
                       • `/move e1-g1` (рокировка)
                       """;
            }

            String notation = parts[1].trim();

            // Выполняем ход через API
            log.info("Ход {} в игре {} от chatId={}", notation, gameId, chatId);
            GameResponse response = apiClient.makeMove(gameId, chatId, notation);

            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                return "❌ *Ошибка выполнения хода!*\n\n" +
                        (response != null ? response.getMessage() : "Недопустимый ход");
            }

            // Обновляем статус сессии
            sessionManager.updateSession(chatId,
                    sessionManager.getSession(chatId).getPlayerColor(),
                    response.getStatus()
            );

            // Формируем ответ
            return formatMoveResponse(response, notation);

        } catch (Exception e) {
            log.error("Ошибка выполнения хода: {}", e.getMessage(), e);
            return "❌ *Ошибка хода:* " + e.getMessage();
        }
    }

    /**
     * 📊 КОМАНДА /BOARD
     */
    private String handleBoard(long chatId) {
        try {
            String gameId = sessionManager.getCurrentGameId(chatId);
            if (gameId == null) {
                return """
                       ❌ *У вас нет активной игры!*
                       
                       Посмотреть доступные игры: `/listgames`
                       """;
            }

            // Получаем текущее состояние игры
            GameResponse response = apiClient.getGameState(gameId, chatId);

            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                return "❌ *Ошибка получения доски!*";
            }

            // Обновляем статус сессии
            sessionManager.updateSession(chatId,
                    sessionManager.getSession(chatId).getPlayerColor(),
                    response.getStatus()
            );

            return formatBoardResponse(response);

        } catch (Exception e) {
            log.error("Ошибка получения доски: {}", e.getMessage(), e);
            return "❌ *Ошибка:* " + e.getMessage();
        }
    }

    /**
     * 🧪 КОМАНДА /STATUS
     */
    private String handleStatus() {
        try {
            String apiStatus = apiClient.testApi();

            // Статистика бота
            int activeSessions = sessionManager.getActiveSessionsCount();

            return """
                   📊 *Статус системы*
                   
                   %s
                   
                   🤖 *Статистика бота:*
                   • Активных сессий: %d
                   • Память: ~%dMB
                   
                   ⚙️ *Команды:*
                   /newgame - Создать игру
                   /listgames - Список игр
                   /help - Помощь
                   """.formatted(
                    apiStatus,
                    activeSessions,
                    Runtime.getRuntime().totalMemory() / (1024 * 1024)
            );

        } catch (Exception e) {
            log.error("Ошибка проверки статуса: {}", e.getMessage(), e);
            return "❌ *Ошибка проверки статуса:* " + e.getMessage();
        }
    }

    /**
     * 🏳️ КОМАНДА /RESIGN (сдаться)
     */
    private String handleResign(long chatId) {
        // TODO: Реализовать сдачу через API
        return """
               ⚠️ *Сдача пока не реализована*
               
               Чтобы завершить игру:
               1. Дождитесь конца партии
               2. Или договоритесь о ничье с соперником
               
               Продолжить игру: `/board`
               """;
    }

    /**
     * 📝 КОМАНДА /MOVES (возможные ходы)
     */
    private String handleLegalMoves(long chatId) {
        try {
            String gameId = sessionManager.getCurrentGameId(chatId);
            if (gameId == null) {
                return "❌ *У вас нет активной игры!*";
            }

            // Получаем возможные ходы из API
            List<String> legalMoves = apiClient.getLegalMoves(gameId, chatId);

            if (legalMoves.isEmpty()) {
                return """
                   🤷 *Нет доступных ходов*
                   
                   Возможно:
                   1. Не ваша очередь ходить
                   2. Игра завершена
                   3. Ошибка получения данных
                   
                   Проверьте статус: `/board`
                   """;
            }

            // Создаем форматированный список
            StringBuilder movesList = new StringBuilder();
            movesList.append("📋 *Возможные ходы:*\n\n");

            int count = 0;
            for (String move : legalMoves) {
                movesList.append("• `").append(move).append("`\n");
                count++;
                if (count >= 20) { // Ограничиваем количество
                    movesList.append("\n... и еще ").append(legalMoves.size() - 20).append(" ходов");
                    break;
                }
            }

            movesList.append("\n🎯 *Использование:* `/move [ход]`");
            movesList.append("\n📖 *Пример:* `/move e2-e4`");

            return movesList.toString();

        } catch (Exception e) {
            log.error("Ошибка получения возможных ходов: {}", e.getMessage(), e);
            return "❌ *Ошибка:* " + e.getMessage();
        }
    }



    /**
     * 🤔 НЕИЗВЕСТНАЯ КОМАНДА
     */
    private String handleUnknownCommand(String text) {
        return """
               🤔 *Неизвестная команда:* `%s`
               
               📋 *Доступные команды:*
               /start - Начало работы
               /newgame - Создать игру
               /listgames - Список игр
               /joingame [ID] - Присоединиться
               /move [ход] - Сделать ход
               /board - Показать доску
               /status - Статус сервера
               /help - Помощь
               
               📖 *Пример:* `/newgame`
               """.formatted(text);
    }

    /**
     * 🎯 ФОРМАТИРОВАНИЕ ОТВЕТА НА ХОД
     */
    private String formatMoveResponse(GameResponse response, String notation) {
        StringBuilder sb = new StringBuilder();

        sb.append("✅ *Ход выполнен:* `").append(notation).append("`\n\n");

        if (response.getMessage() != null) {
            sb.append("💬 *").append(response.getMessage()).append("*\n\n");
        }

        sb.append("📊 *Статус:* ").append(response.getStatus()).append("\n");
        sb.append("♟️ *Очередь:* ").append(response.getCurrentTurn()).append("\n\n");

        if (response.getBoard() != null) {
            sb.append("🎮 *Текущая доска:*\n```\n")
                    .append(response.getBoard())
                    .append("\n```\n\n");
        }

        // Информация о игроках
        if (response.getWhitePlayer() != null) {
            sb.append("⚪ *Белые:* ").append(response.getWhitePlayer().getName());
            if (response.getWhitePlayer().getRating() != null) {
                sb.append(" (Рейтинг: ").append(response.getWhitePlayer().getRating()).append(")");
            }
            sb.append("\n");
        }

        if (response.getBlackPlayer() != null) {
            sb.append("⚫ *Черные:* ").append(response.getBlackPlayer().getName());
            if (response.getBlackPlayer().getRating() != null) {
                sb.append(" (Рейтинг: ").append(response.getBlackPlayer().getRating()).append(")");
            }
            sb.append("\n");
        }

        sb.append("\n🔄 *Обновить доску:* `/board`");

        return sb.toString();
    }

    /**
     * 📊 ФОРМАТИРОВАНИЕ ОТВЕТА С ДОСКОЙ
     */
    private String formatBoardResponse(GameResponse response) {
        StringBuilder sb = new StringBuilder();

        sb.append("📊 *Текущее состояние игры*\n\n");

        if (response.getMessage() != null) {
            sb.append("💬 *").append(response.getMessage()).append("*\n\n");
        }

        sb.append("🆔 *ID:* `").append(response.getGameId()).append("`\n");
        sb.append("📊 *Статус:* ").append(response.getStatus()).append("\n");
        sb.append("♟️ *Очередь:* ").append(response.getCurrentTurn()).append("\n\n");

        if (response.getBoard() != null) {
            sb.append("🎮 *Доска:*\n```\n")
                    .append(response.getBoard())
                    .append("\n```\n\n");
        }

        sb.append("🎯 *Сделать ход:* `/move [ход]`\n");
        sb.append("📖 *Пример:* `/move e2-e4`");

        return sb.toString();
    }
    /**
     * 🤝 КОМАНДА /DRAW
     */
    private String handleDraw(long chatId, String text, String userName) {
        try {
            String gameId = sessionManager.getCurrentGameId(chatId);
            if (gameId == null) {
                return "❌ *У вас нет активной игры!*";
            }

            String[] parts = text.split("\\s+");

            if (parts.length == 1) {
                // /draw - предложить ничью
                return handleOfferDraw(chatId, gameId);
            } else if (parts.length >= 2) {
                // /draw accept / draw decline
                String action = parts[1].toLowerCase();
                return handleDrawResponse(chatId, gameId, action);
            } else {
                return """
                       ❌ *Некорректное использование!*
                       
                       Использование:
                       • `/draw` - предложить ничью
                       • `/draw accept` - принять ничью
                       • `/draw decline` - отклонить ничью
                       """;
            }

        } catch (Exception e) {
            log.error("Ошибка обработки ничьи: {}", e.getMessage(), e);
            return "❌ *Ошибка:* " + e.getMessage();
        }
    }

    private String handleOfferDraw(long chatId, String gameId) {
        GameResponse response = apiClient.offerDraw(gameId, chatId);

        if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
            return "❌ *Не удалось предложить ничью!*\n" +
                    (response != null ? response.getMessage() : "");
        }

        return """
               🤝 *Ничья предложена!*
               
               Ожидайте ответа от соперника.
               
               📊 Статус: %s
               💬 Сообщение: %s
               
               Соперник может:
               • Принять: `/draw accept`
               • Отклонить: `/draw decline`
               """.formatted(
                response.getStatus(),
                response.getMessage()
        );
    }

    private String handleDrawResponse(long chatId, String gameId, String action) {
        if (!action.equals("accept") && !action.equals("decline")) {
            return """
                   ❌ *Некорректное действие!*
                   
                   Используйте:
                   • `/draw accept` - принять
                   • `/draw decline` - отклонить
                   """;
        }

        boolean accept = action.equals("accept");
        GameResponse response = apiClient.respondToDraw(gameId, chatId, accept);

        if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
            return "❌ *Ошибка обработки ничьи!*\n" +
                    (response != null ? response.getMessage() : "");
        }

        if (accept) {
            // Игра завершена ничьей
            sessionManager.removeSession(chatId);

            return """
                   🤝 *Ничья принята!*
                   
                   🎉 Игра завершена вничью!
                   
                   📊 Статус: %s
                   💬 Сообщение: %s
                   
                   🎮 Начать новую игру: `/newgame`
                   """.formatted(
                    response.getStatus(),
                    response.getMessage()
            );
        } else {
            return """
                   ❌ *Ничья отклонена!*
                   
                   Игра продолжается.
                   
                   💬 Сообщение: %s
                   
                   🎯 Продолжить игру: `/board`
                   """.formatted(response.getMessage());
        }
    }


    /**
     * 🔄 СООБЩЕНИЕ О ОЧЕРЕДИ ХОДА
     */
    private String getTurnMessage(GameResponse response) {
        if ("CHECKMATE".equals(response.getStatus())) {
            return "🎉 *Игра окончена! МАТ!*";
        } else if ("STALEMATE".equals(response.getStatus())) {
            return "🤝 *Пат! Ничья.*";
        } else if ("DRAW".equals(response.getStatus())) {
            return "🤝 *Ничья!*";
        } else if ("CHECK".equals(response.getStatus())) {
            return "⚠️ *ШАХ!* Сделайте ход, чтобы уйти от шаха.";
        } else {
            return "🎮 *Игра началась!* Сделайте ход.";
        }
    }
}