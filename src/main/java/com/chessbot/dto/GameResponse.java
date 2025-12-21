package com.chessbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для ответа от Chess API
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameResponse {
    private Boolean success;
    private String gameId;
    private String status;
    private String message;
    private String board;
    private String currentTurn;
    private PlayerInfo whitePlayer;
    private PlayerInfo blackPlayer;
    private Object additionalInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerInfo {
        private Long id;
        private String name;
        private String color;
        private Integer rating;
    }
}
