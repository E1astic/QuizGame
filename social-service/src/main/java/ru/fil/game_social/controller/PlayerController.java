package ru.fil.game_social.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fil.game_social.dto.PlayerRegisterRequest;
import ru.fil.game_social.service.PlayerService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    public ResponseEntity<String> createPlayerInfo(
            Principal principal,
            @RequestBody PlayerRegisterRequest registerRequest
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return playerService.register(userId, registerRequest).isPresent()
                ? ResponseEntity.ok("Успешное сохранение")
                : ResponseEntity.badRequest().body("Игрок уже зарегистрирован");
    }
}
