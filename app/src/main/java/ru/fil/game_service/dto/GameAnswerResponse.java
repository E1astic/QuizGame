package ru.fil.game_service.dto;

import java.util.UUID;

public record GameAnswerResponse(UUID gameId, UUID questionId, UUID teamId, boolean correct, String message) {
}
