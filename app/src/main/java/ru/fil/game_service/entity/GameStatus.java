package ru.fil.game_service.entity;

public enum GameStatus {
    WAITING,      // Ожидание начала игры (команды могут присоединяться)
    IN_PROGRESS,  // Игра идет
    FINISHED      // Игра завершена
}
