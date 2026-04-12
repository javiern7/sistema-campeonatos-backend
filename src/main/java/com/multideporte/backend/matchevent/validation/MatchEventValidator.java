package com.multideporte.backend.matchevent.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.matchevent.entity.MatchEvent;
import com.multideporte.backend.matchevent.entity.MatchEventStatus;
import com.multideporte.backend.matchevent.entity.MatchEventType;
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
public class MatchEventValidator {

    private static final EnumSet<MatchGameStatus> EVENT_ENABLED_MATCH_STATUSES = EnumSet.of(
            MatchGameStatus.SCHEDULED,
            MatchGameStatus.PLAYED
    );

    private final MatchGameRepository matchGameRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final PlayerRepository playerRepository;
    private final TeamPlayerRosterRepository teamPlayerRosterRepository;

    public MatchGame requireExistingMatch(Long matchId) {
        return matchGameRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("MatchGame no encontrado con id: " + matchId));
    }

    public MatchGame requireEditableMatch(Long matchId) {
        MatchGame match = requireExistingMatch(matchId);
        if (!EVENT_ENABLED_MATCH_STATUSES.contains(match.getStatus())) {
            throw new BusinessException("Los eventos solo pueden registrarse sobre partidos SCHEDULED o PLAYED");
        }
        return match;
    }

    public void validateForCreateOrUpdate(
            MatchGame match,
            MatchEventType eventType,
            Long tournamentTeamId,
            Long playerId,
            Long relatedPlayerId,
            Integer eventMinute,
            Integer eventSecond,
            Integer eventValue
    ) {
        validateEventTypeShape(eventType, tournamentTeamId, playerId, relatedPlayerId, eventMinute, eventValue);
        validateSecond(eventSecond);
        validateTeamAndPlayers(match, tournamentTeamId, playerId, relatedPlayerId);
    }

    public void validateForUpdate(MatchEvent event) {
        if (event.getStatus() == MatchEventStatus.ANNULLED) {
            throw new BusinessException("No se puede editar un evento anulado");
        }
    }

    public void validateForAnnul(MatchEvent event, Long matchId) {
        if (!Objects.equals(event.getMatchId(), matchId)) {
            throw new BusinessException("El evento indicado no pertenece al partido solicitado");
        }
        if (event.getStatus() == MatchEventStatus.ANNULLED) {
            throw new BusinessException("El evento ya se encuentra anulado");
        }
    }

    private void validateEventTypeShape(
            MatchEventType eventType,
            Long tournamentTeamId,
            Long playerId,
            Long relatedPlayerId,
            Integer eventMinute,
            Integer eventValue
    ) {
        if (eventType == null) {
            throw new BusinessException("eventType es obligatorio");
        }

        switch (eventType) {
            case SCORE -> {
                requireTeamPlayerMinute(tournamentTeamId, playerId, eventMinute, "SCORE");
                if (eventValue == null || eventValue <= 0) {
                    throw new BusinessException("SCORE requiere eventValue mayor que 0");
                }
            }
            case YELLOW_CARD, RED_CARD -> requireTeamPlayerMinute(tournamentTeamId, playerId, eventMinute, eventType.name());
            case SUBSTITUTION -> {
                requireTeamPlayerMinute(tournamentTeamId, playerId, eventMinute, "SUBSTITUTION");
                if (relatedPlayerId == null) {
                    throw new BusinessException("SUBSTITUTION requiere relatedPlayerId");
                }
                if (Objects.equals(playerId, relatedPlayerId)) {
                    throw new BusinessException("SUBSTITUTION requiere jugadores distintos");
                }
            }
            case INCIDENT, NOTE -> {
                if (eventMinute != null && eventMinute < 0) {
                    throw new BusinessException(eventType.name() + " no admite eventMinute negativo");
                }
            }
        }
    }

    private void requireTeamPlayerMinute(Long tournamentTeamId, Long playerId, Integer eventMinute, String eventType) {
        if (tournamentTeamId == null) {
            throw new BusinessException(eventType + " requiere tournamentTeamId");
        }
        if (playerId == null) {
            throw new BusinessException(eventType + " requiere playerId");
        }
        if (eventMinute == null) {
            throw new BusinessException(eventType + " requiere eventMinute");
        }
    }

    private void validateSecond(Integer eventSecond) {
        if (eventSecond != null && (eventSecond < 0 || eventSecond > 59)) {
            throw new BusinessException("eventSecond debe estar entre 0 y 59");
        }
    }

    private void validateTeamAndPlayers(
            MatchGame match,
            Long tournamentTeamId,
            Long playerId,
            Long relatedPlayerId
    ) {
        if (tournamentTeamId == null) {
            if (playerId != null || relatedPlayerId != null) {
                throw new BusinessException("playerId y relatedPlayerId requieren tournamentTeamId");
            }
            return;
        }

        TournamentTeam tournamentTeam = tournamentTeamRepository.findById(tournamentTeamId)
                .orElseThrow(() -> new BusinessException("El tournamentTeamId enviado no existe"));
        if (!Objects.equals(tournamentTeam.getTournamentId(), match.getTournamentId())) {
            throw new BusinessException("El tournamentTeamId enviado no pertenece al torneo del partido");
        }
        if (!Objects.equals(match.getHomeTournamentTeamId(), tournamentTeamId)
                && !Objects.equals(match.getAwayTournamentTeamId(), tournamentTeamId)) {
            throw new BusinessException("El tournamentTeamId enviado no participa en el partido indicado");
        }

        validatePlayerMembership(match, tournamentTeamId, playerId, "playerId");
        validatePlayerMembership(match, tournamentTeamId, relatedPlayerId, "relatedPlayerId");
    }

    private void validatePlayerMembership(MatchGame match, Long tournamentTeamId, Long playerId, String fieldName) {
        if (playerId == null) {
            return;
        }
        if (!playerRepository.existsById(playerId)) {
            throw new BusinessException("El " + fieldName + " enviado no existe");
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
            throw new BusinessException("El " + fieldName + " no pertenece al roster elegible del tournamentTeam para la fecha del partido");
        }
    }
}
