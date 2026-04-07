package ru.fil.game_service.dto;

import java.util.UUID;

public record GameCreateResponse(UUID gameId, String quizName) {
}
