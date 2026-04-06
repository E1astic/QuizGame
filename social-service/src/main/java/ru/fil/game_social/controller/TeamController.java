package ru.fil.game_social.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fil.game_social.dto.TeamCreateRequest;
import ru.fil.game_social.dto.TeamDeleteRequest;
import ru.fil.game_social.dto.TeamInviteRequest;
import ru.fil.game_social.dto.TeamJoinRequest;
import ru.fil.game_social.service.TeamInvitationService;
import ru.fil.game_social.service.TeamService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamInvitationService teamInvitationService;

    @PostMapping
    public ResponseEntity<String> createTeam(
            Principal principal,
            @RequestBody TeamCreateRequest createRequest
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return teamService.createTeam(userId, createRequest).isPresent()
                ? ResponseEntity.ok("Успешное создание")
                : ResponseEntity.badRequest().body("Команда с таким названием уже существует или вы уже состоите в команде");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteTeam(
            Principal principal,
            @RequestBody TeamDeleteRequest deleteRequest
    ) {
        UUID userId = UUID.fromString(principal.getName());
        if (teamService.deleteTeam(userId, deleteRequest)) {
            return ResponseEntity.ok("Команда успешно удалена");
        }
        return ResponseEntity.badRequest().body("Команда не была удалена");
    }

    @PostMapping("/invitations")
    public ResponseEntity<String> createInvitation(
            Principal principal,
            @RequestBody TeamInviteRequest inviteRequest
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return teamInvitationService.createInvitation(userId, inviteRequest).isPresent()
                ? ResponseEntity.ok("Приглашение создано")
                : ResponseEntity.badRequest().body("Приглашение не создано");
    }

    @PostMapping("/join")
    public ResponseEntity<String> joinToTeam(
            Principal principal,
            @RequestBody TeamJoinRequest joinRequest
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return teamService.joinToTeam(userId, joinRequest).isPresent()
                ? ResponseEntity.ok("Успешное присоединение к команде")
                : ResponseEntity.badRequest().body("Нет пришлашения или грок уже состоит в команде");
    }
}
