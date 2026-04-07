package ru.fil.game_social.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "players")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Player {

    @Id
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @Column(name = "games_played", nullable = false)
    @Builder.Default
    private Integer gamesPlayed = 0;

    @Column(name = "wins", nullable = false)
    @Builder.Default
    private Integer wins = 0;

    @Column(name = "rating", nullable = false)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PlayerToTeam> teams = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
