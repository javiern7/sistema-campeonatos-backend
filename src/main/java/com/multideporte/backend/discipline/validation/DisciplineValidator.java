package com.multideporte.backend.discipline.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.discipline.entity.DisciplinaryIncident;
import com.multideporte.backend.discipline.entity.DisciplinarySanctionType;
import com.multideporte.backend.discipline.repository.DisciplinarySanctionRepository;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DisciplineValidator {

    private static final EnumSet<MatchGameStatus> DISCIPLINE_ENABLED_MATCH_STATUSES = EnumSet.of(
            MatchGameStatus.PLAYED,
            MatchGameStatus.FORFEIT
    );

    private final MatchGameRepository matchGameRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final PlayerRepository playerRepository;
    private final TeamPlayerRosterRepository teamPlayerRosterRepository;
    private final DisciplinarySanctionRepository disciplinarySanctionRepository;

    public MatchGame requireValidMatch(Long matchId) {
        MatchGame match = matchGameRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("MatchGame no encontrado con id: " + matchId));
        if (!DISCIPLINE_ENABLED_MATCH_STATUSES.contains(match.getStatus())) {
            throw new BusinessException("La disciplina solo puede registrarse sobre partidos cerrados PLAYED o FORFEIT");
        }
        return match;
    }

    public void validateIncidentCreate(MatchGame match, Long tournamentTeamId, Long playerId) {
        TournamentTeam tournamentTeam = tournamentTeamRepository.findById(tournamentTeamId)
                .orElseThrow(() -> new BusinessException("El tournamentTeamId enviado no existe"));
        if (!Objects.equals(tournamentTeam.getTournamentId(), match.getTournamentId())) {
            throw new BusinessException("El tournamentTeamId enviado no pertenece al torneo del partido");
        }
        if (!Objects.equals(match.getHomeTournamentTeamId(), tournamentTeamId)
                && !Objects.equals(match.getAwayTournamentTeamId(), tournamentTeamId)) {
            throw new BusinessException("El tournamentTeamId enviado no participa en el partido indicado");
        }
        if (!playerRepository.existsById(playerId)) {
            throw new BusinessException("El playerId enviado no existe");
        }

        LocalDate referenceDate = match.getScheduledAt() != null
                ? match.getScheduledAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate()
                : LocalDate.now(ZoneOffset.UTC);
        boolean rosterMember = teamPlayerRosterRepository.existsEligibleRosterMembership(
                tournamentTeamId,
                playerId,
                referenceDate
        );
        if (!rosterMember) {
            throw new BusinessException("El jugador no pertenece al roster elegible del tournamentTeam para la fecha del partido");
        }
    }

    public void validateSanctionCreate(
            MatchGame match,
            DisciplinaryIncident incident,
            DisciplinarySanctionType sanctionType,
            Integer matchesToServe
    ) {
        if (!Objects.equals(incident.getMatchId(), match.getId())) {
            throw new BusinessException("La incidencia indicada no pertenece al partido solicitado");
        }
        if (!Objects.equals(incident.getTournamentId(), match.getTournamentId())) {
            throw new BusinessException("La incidencia indicada no pertenece al torneo solicitado");
        }
        if (disciplinarySanctionRepository.existsByIncidentIdAndSanctionType(incident.getId(), sanctionType)) {
            throw new BusinessException("Ya existe una sancion del mismo tipo para la incidencia indicada");
        }

        int normalizedMatchesToServe = matchesToServe == null ? 0 : matchesToServe;
        if (sanctionType == DisciplinarySanctionType.ANOTACION_DISCIPLINARIA && normalizedMatchesToServe != 0) {
            throw new BusinessException("ANOTACION_DISCIPLINARIA no admite matchesToServe mayor que 0");
        }
        if (sanctionType == DisciplinarySanctionType.SUSPENSION_PROXIMO_PARTIDO && normalizedMatchesToServe <= 0) {
            throw new BusinessException("SUSPENSION_PROXIMO_PARTIDO requiere matchesToServe mayor que 0");
        }
    }
}
