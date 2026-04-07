package ru.fil.game_service.dto;

import java.util.UUID;

public record GameStartResponse(UUID gameId, boolean success, String message) {
}
