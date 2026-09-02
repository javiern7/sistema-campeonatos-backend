package com.multideporte.backend.standing.tiebreak.entity;
import jakarta.persistence.*; import java.time.OffsetDateTime; import java.util.List; import lombok.Getter; import lombok.Setter; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes;
@Entity @Table(name="standing_tie_break_draw") @Getter @Setter public class StandingTieBreakDraw {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="tournament_id",nullable=false) private Long tournamentId; @Column(name="stage_id",nullable=false) private Long stageId; @Column(name="group_id",nullable=false) private Long groupId;
@JdbcTypeCode(SqlTypes.JSON) @Column(name="affected_team_ids",nullable=false,columnDefinition="jsonb") private List<Long> affectedTeamIds;
@JdbcTypeCode(SqlTypes.JSON) @Column(name="resulting_order_team_ids",nullable=false,columnDefinition="jsonb") private List<Long> resultingOrderTeamIds;
@Column(nullable=false) private String reason; @Column(name="recorded_by_user_id",nullable=false) private Long recordedByUserId; @Column(name="recorded_at",nullable=false) private OffsetDateTime recordedAt;
}
