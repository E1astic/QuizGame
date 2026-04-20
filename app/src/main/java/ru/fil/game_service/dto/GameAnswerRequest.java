package ru.fil.game_service.dto;

import java.util.UUID;

public record GameAnswerRequest(UUID gameId, UUID questionId, UUID answerId, UUID teamId) {
}
