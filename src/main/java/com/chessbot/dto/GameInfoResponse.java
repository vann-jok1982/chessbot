package com.chessbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для списка игр
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameInfoResponse {
    private String gameId;
    private String whitePlayerName;
    private String createdAt;
}
