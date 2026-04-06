package ru.fil.game_social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.fil.game_social.entity.PlayerTeamId;
import ru.fil.game_social.entity.PlayerToTeam;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerTeamRepository extends JpaRepository<PlayerToTeam, PlayerTeamId> {

    Optional<PlayerToTeam> findByPlayerId(UUID playerId);

    Optional<PlayerToTeam> findByPlayerIdAndTeamId(UUID playerId, UUID teamId);

    @Query(value = """
        INSERT INTO players_to_teams (player_id, team_id, is_capitan, joined_at)
        VALUES (:playerId, :teamId, :isCapitan, :joinedAt)
        """, nativeQuery = true)
    @Modifying
    int saveNative(@Param("playerId") UUID playerId, @Param("teamId") UUID teamId,
                   @Param("isCapitan") boolean isCapitan, @Param("joinedAt") OffsetDateTime joinedAt);
}
