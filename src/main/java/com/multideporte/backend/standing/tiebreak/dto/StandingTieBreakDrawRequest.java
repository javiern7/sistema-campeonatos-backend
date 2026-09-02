package com.multideporte.backend.standing.tiebreak.dto;
import jakarta.validation.constraints.*; import java.util.List;
public record StandingTieBreakDrawRequest(@NotNull Long tournamentId,@NotNull Long stageId,@NotNull Long groupId,@NotEmpty List<@NotNull Long> affectedTeamIds,@NotEmpty List<@NotNull Long> resultingOrderTeamIds,@NotBlank String reason) {}
