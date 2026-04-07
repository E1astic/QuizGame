package ru.fil.app.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmitRequest {
    private UUID gameId;
    private UUID teamId;
    private UUID questionId;
    private UUID answerId;
}
