package com.chessbot.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎮 Менеджер состояния игр для бота
 * Хранит информацию о текущих играх пользователей в памяти
 */
@Slf4j
@Component
public class GameSessionManager {

    @Getter
    @Setter
    public static class GameSession {
        private String gameId;
        private Long chatId;
        private Long playerId;
        private String playerColor; // "WHITE" или "BLACK"
        private String opponentName;
        private LocalDateTime lastActivity;
        private String gameStatus; // "ACTIVE", "CHECK", "CHECKMATE" и т.д.

        public GameSession(String gameId, Long chatId, Long playerId) {
            this.gameId = gameId;
            this.chatId = chatId;
            this.playerId = playerId;
            this.lastActivity = LocalDateTime.now();
        }

        public void updateActivity() {
            this.lastActivity = LocalDateTime.now();
        }

        public boolean isActive() {
            return "ACTIVE".equals(gameStatus) || "CHECK".equals(gameStatus);
        }
    }

    // Хранилище сессий: chatId -> GameSession
    private final Map<Long, GameSession> activeSessions = new ConcurrentHashMap<>();

    // Хранилище по gameId: gameId -> chatId (для поиска)
    private final Map<String, Long> gameToChatMap = new ConcurrentHashMap<>();

    /**
     * СОЗДАТЬ НОВУЮ СЕССИЮ ИГРЫ
     */
    public void createSession(String gameId, Long chatId, Long playerId) {
        GameSession session = new GameSession(gameId, chatId, playerId);
        activeSessions.put(chatId, session);
        gameToChatMap.put(gameId, chatId);
        log.info("Создана новая сессия: gameId={}, chatId={}", gameId, chatId);
    }

    /**
     * ПОЛУЧИТЬ СЕССИЮ ПО CHAT_ID
     */
    public GameSession getSession(Long chatId) {
        GameSession session = activeSessions.get(chatId);
        if (session != null) {
            session.updateActivity();
        }
        return session;
    }

    /**
     * ПОЛУЧИТЬ СЕССИЮ ПО GAME_ID
     */
    public GameSession getSessionByGameId(String gameId) {
        Long chatId = gameToChatMap.get(gameId);
        if (chatId != null) {
            return getSession(chatId);
        }
        return null;
    }

    /**
     * ОБНОВИТЬ ИНФОРМАЦИЮ О СЕССИИ
     */
    public void updateSession(Long chatId, String playerColor, String gameStatus) {
        GameSession session = activeSessions.get(chatId);
        if (session != null) {
            session.setPlayerColor(playerColor);
            session.setGameStatus(gameStatus);
            session.updateActivity();
            log.debug("Обновлена сессия chatId={}: color={}, status={}",
                    chatId, playerColor, gameStatus);
        }
    }

    /**
     * УДАЛИТЬ СЕССИЮ
     */
    public void removeSession(Long chatId) {
        GameSession session = activeSessions.get(chatId);
        if (session != null) {
            gameToChatMap.remove(session.getGameId());
            activeSessions.remove(chatId);
            log.info("Удалена сессия: chatId={}, gameId={}", chatId, session.getGameId());
        }
    }

    /**
     * ОЧИСТИТЬ НЕАКТИВНЫЕ СЕССИИ
     */
    public void cleanupInactiveSessions(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);

        activeSessions.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastActivity().isBefore(cutoff)) {
                gameToChatMap.remove(entry.getValue().getGameId());
                log.info("Очищена неактивная сессия: chatId={}, gameId={}",
                        entry.getKey(), entry.getValue().getGameId());
                return true;
            }
            return false;
        });
    }

    /**
     * ПРОВЕРИТЬ ЕСТЬ ЛИ АКТИВНАЯ ИГРА
     */
    public boolean hasActiveGame(Long chatId) {
        GameSession session = activeSessions.get(chatId);
        return session != null && session.isActive();
    }

    /**
     * ПОЛУЧИТЬ ID ТЕКУЩЕЙ ИГРЫ
     */
    public String getCurrentGameId(Long chatId) {
        GameSession session = activeSessions.get(chatId);
        return session != null ? session.getGameId() : null;
    }

    /**
     * КОЛИЧЕСТВО АКТИВНЫХ СЕССИЙ
     */
    public int getActiveSessionsCount() {
        return activeSessions.size();
    }
}