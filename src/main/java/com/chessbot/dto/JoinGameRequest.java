package com.chessbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для присоединения к игре
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinGameRequest {
    private Long playerId;
    private String playerName;
}
