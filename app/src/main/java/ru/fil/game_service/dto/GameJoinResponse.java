package ru.fil.game_service.dto;

import java.util.UUID;

public record GameJoinResponse(UUID gameId, UUID teamId, boolean success, String message) {
}
