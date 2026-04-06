package ru.fil.game_social.repository;

import org.assertj.core.data.Offset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.fil.game_social.entity.TeamInvitation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, UUID> {

    List<TeamInvitation> findByRecipientId(UUID recipientId);

    Optional<TeamInvitation> findByRecipientIdAndTeamId(UUID recipientId, UUID teamId);

    @Query(value = """
        INSERT INTO team_invitations(sender_id, recipient_id, team_id, created_at)
        VALUES (:senderId, :recipientId, :teamId, :createdAt)
        """, nativeQuery = true)
    @Modifying
    int saveNative(@Param("senderId") UUID senderId, @Param("recipientId") UUID recipientId,
                   @Param("teamId")UUID teamId, @Param("createdAt") OffsetDateTime createdAt);
}
