package ru.fil.game_social.dto;

import java.util.UUID;

public record TeamInviteRequest(UUID teamId, UUID recipientId) {
}
