package com.chessbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для создания игры
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameRequest {
    private Long playerId;
    private String playerName;
}

