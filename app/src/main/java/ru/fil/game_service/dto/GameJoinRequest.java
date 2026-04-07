package ru.fil.game_service.dto;

import java.util.UUID;

public record GameJoinRequest(UUID gameId, UUID teamId) {
}
