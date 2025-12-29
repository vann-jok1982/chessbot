package com.chessbot.service;

import com.chessbot.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiClient {

    private final RestTemplate restTemplate;

    private final String API_BASE_URL = "http://localhost:8080/api/games";

    /**
     * 🆕 СОЗДАТЬ НОВУЮ ИГРУ
     */
    public GameResponse createGame(Long playerId, String playerName) {
        String url = API_BASE_URL;

        CreateGameRequest request = new CreateGameRequest(playerId, playerName);

        try {
            log.info("Отправка запроса на создание игры: {}", request);
            ResponseEntity<GameResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    GameResponse.class
            );

            GameResponse gameResponse = response.getBody();
            log.info("Ответ от API (создание игры): {}", gameResponse);
            return gameResponse;

        } catch (Exception e) {
            log.error("Ошибка при создании игры: {}", e.getMessage(), e);
            return createErrorResponse("Ошибка создания игры: " + e.getMessage());
        }
    }

    /**
     * 📋 ПОЛУЧИТЬ СПИСОК ОЖИДАЮЩИХ ИГР
     */
    public List<GameInfoResponse> getWaitingGames() {
        String url = API_BASE_URL + "/waiting";

        try {
            log.info("Запрос списка ожидающих игр");
            ResponseEntity<GameInfoResponse[]> response = restTemplate.getForEntity(
                    url,
                    GameInfoResponse[].class
            );

            List<GameInfoResponse> games = List.of(response.getBody());
            log.info("Получено {} ожидающих игр", games.size());
            return games;

        } catch (Exception e) {
            log.error("Ошибка при получении списка игр: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 🤝 ПРИСОЕДИНИТЬСЯ К ИГРЕ
     */
    public GameResponse joinGame(String gameId, Long playerId, String playerName) {
        String url = API_BASE_URL + "/" + gameId + "/join";

        JoinGameRequest request = new JoinGameRequest(playerId, playerName);

        try {
            log.info("Отправка запроса на присоединение к игре {} игроком {}", gameId, playerId);
            ResponseEntity<GameResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    GameResponse.class
            );

            GameResponse gameResponse = response.getBody();
            log.info("Ответ от API (присоединение): {}", gameResponse);
            return gameResponse;

        } catch (Exception e) {
            log.error("Ошибка при присоединении к игре: {}", e.getMessage(), e);
            return createErrorResponse("Ошибка присоединения: " + e.getMessage());
        }
    }

    /**
     * ♟️ СДЕЛАТЬ ХОД
     */
    public GameResponse makeMove(String gameId, Long playerId, String notation) {
        String url = API_BASE_URL + "/" + gameId + "/move";

        MoveRequest request = new MoveRequest(playerId, notation);

        try {
            log.info("Отправка хода {} в игру {} от игрока {}", notation, gameId, playerId);
            ResponseEntity<GameResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    GameResponse.class
            );

            GameResponse gameResponse = response.getBody();
            log.info("Ответ от API (ход): {}", gameResponse);
            return gameResponse;

        } catch (Exception e) {
            log.error("Ошибка при выполнении хода: {}", e.getMessage(), e);
            return createErrorResponse("Ошибка хода: " + e.getMessage());
        }
    }

    /**
     * 📊 ПОЛУЧИТЬ ТЕКУЩЕЕ СОСТОЯНИЕ ИГРЫ
     */
    public GameResponse getGameState(String gameId, Long playerId) {
        String url = API_BASE_URL + "/" + gameId + "?playerId=" + playerId;

        try {
            log.info("Запрос состояния игры {} для игрока {}", gameId, playerId);
            ResponseEntity<GameResponse> response = restTemplate.getForEntity(
                    url,
                    GameResponse.class
            );

            GameResponse gameResponse = response.getBody();
            log.info("Ответ от API (состояние игры): {}", gameResponse);
            return gameResponse;

        } catch (Exception e) {
            log.error("Ошибка при получении состояния игры: {}", e.getMessage(), e);
            return createErrorResponse("Ошибка получения игры: " + e.getMessage());
        }
    }

    /**
     * 🧪 ПРОВЕРИТЬ СТАТУС API
     */
    public String testApi() {
        String url = "http://localhost:8081/api/games/test";

        try {
            log.info("Проверка доступности API");
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("API доступен: {}", response.getBody());
            return "✅ API работает: " + response.getBody();

        } catch (Exception e) {
            log.error("API недоступен: {}", e.getMessage());
            return "❌ API недоступен: " + e.getMessage();
        }
    }
    /**
     * 🤝 ПРЕДЛОЖИТЬ НИЧЬЮ
     */
    public GameResponse offerDraw(String gameId, Long playerId) {
        String url = "http://localhost:8081/api/games/" + gameId + "/draw/offer?playerId=" + playerId;

        try {
            log.info("Предложение ничьи в игре {} от игрока {}", gameId, playerId);
            ResponseEntity<GameResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    GameResponse.class
            );

            GameResponse gameResponse = response.getBody();
            log.info("Ответ на предложение ничьи: {}", gameResponse);
            return gameResponse;

        } catch (Exception e) {
            log.error("Ошибка предложения ничьи: {}", e.getMessage(), e);
            return createErrorResponse("Ошибка предложения ничьи: " + e.getMessage());
        }
    }

    /**
     * 🤝 ОТВЕТИТЬ НА ПРЕДЛОЖЕНИЕ НИЧЬЕЙ
     */
    public GameResponse respondToDraw(String gameId, Long playerId, boolean accept) {
        String url = "http://localhost:8081/api/games/" + gameId +
                "/draw/respond?playerId=" + playerId + "&accept=" + accept;

        try {
            log.info("Ответ на ничью в игре {} от игрока {}: {}", gameId, playerId, accept);
            ResponseEntity<GameResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    GameResponse.class
            );

            GameResponse gameResponse = response.getBody();
            log.info("Ответ на принятие/отклонение ничьи: {}", gameResponse);
            return gameResponse;

        } catch (Exception e) {
            log.error("Ошибка ответа на ничью: {}", e.getMessage(), e);
            return createErrorResponse("Ошибка ответа на ничью: " + e.getMessage());
        }
    }
    /**
     * 📋 ПОЛУЧИТЬ ВОЗМОЖНЫЕ ХОДЫ
     */
    public List<String> getLegalMoves(String gameId, Long playerId) {
        String url = API_BASE_URL + "/" + gameId + "?playerId=" + playerId;

        try {
            log.info("Запрос возможных ходов для игры {} игрока {}", gameId, playerId);
            ResponseEntity<GameResponse> response = restTemplate.getForEntity(
                    url,
                    GameResponse.class
            );

            GameResponse gameResponse = response.getBody();
            if (gameResponse != null && gameResponse.getAdditionalInfo() != null) {
                // Предполагаем, что legalMoves есть в additionalInfo
                // Нужно адаптировать под реальную структуру ответа
                @SuppressWarnings("unchecked")
                Map<String, Object> additionalInfo = (Map<String, Object>) gameResponse.getAdditionalInfo();
                if (additionalInfo.containsKey("legalMoves")) {
                    @SuppressWarnings("unchecked")
                    List<String> moves = (List<String>) additionalInfo.get("legalMoves");
                    log.info("Получено {} возможных ходов", moves.size());
                    return moves;
                }
            }

            log.warn("Возможные ходы не найдены в ответе");
            return List.of();

        } catch (Exception e) {
            log.error("Ошибка получения возможных ходов: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * ❌ СОЗДАТЬ ОТВЕТ ОБ ОШИБКЕ
     */
    private GameResponse createErrorResponse(String message) {
        GameResponse errorResponse = new GameResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage(message);
        return errorResponse;
    }
}