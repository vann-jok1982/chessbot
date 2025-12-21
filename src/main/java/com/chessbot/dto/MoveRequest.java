package com.chessbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для хода
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {
    private Long playerId;
    private String notation;
}
