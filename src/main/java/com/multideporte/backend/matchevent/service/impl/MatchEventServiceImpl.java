package com.multideporte.backend.matchevent.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.matchevent.dto.request.MatchEventAnnulRequest;
import com.multideporte.backend.matchevent.dto.request.MatchEventCreateRequest;
import com.multideporte.backend.matchevent.dto.request.MatchEventUpdateRequest;
import com.multideporte.backend.matchevent.dto.response.MatchEventResponse;
import com.multideporte.backend.matchevent.entity.MatchEvent;
import com.multideporte.backend.matchevent.entity.MatchEventStatus;
import com.multideporte.backend.matchevent.repository.MatchEventRepository;
import com.multideporte.backend.matchevent.service.MatchEventService;
import com.multideporte.backend.matchevent.validation.MatchEventValidator;
import com.multideporte.backend.security.user.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchEventServiceImpl implements MatchEventService {

    private final MatchEventRepository matchEventRepository;
    private final MatchEventValidator matchEventValidator;
    private final CurrentUserService currentUserService;

    @Override
    public List<MatchEventResponse> getMatchEvents(Long matchId) {
        matchEventValidator.requireExistingMatch(matchId);
        return matchEventRepository.findAllByMatchIdOrderByEventMinuteAscEventSecondAscCreatedAtAscIdAsc(matchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MatchEventResponse create(Long matchId, MatchEventCreateRequest request) {
        MatchGame match = matchEventValidator.requireEditableMatch(matchId);
        matchEventValidator.validateForCreateOrUpdate(
                match,
                request.eventType(),
                request.tournamentTeamId(),
                request.playerId(),
                request.relatedPlayerId(),
                request.eventMinute(),
                request.eventSecond(),
                request.eventValue()
        );

        MatchEvent event = new MatchEvent();
        event.setMatchId(match.getId());
        event.setTournamentId(match.getTournamentId());
        event.setEventType(request.eventType());
        event.setStatus(MatchEventStatus.ACTIVE);
        applyRequest(event, request);
        event.setCreatedByUserId(currentUserService.requireCurrentUserId());

        return toResponse(matchEventRepository.save(event));
    }

    @Override
    @Transactional
    public MatchEventResponse update(Long matchId, Long eventId, MatchEventUpdateRequest request) {
        MatchGame match = matchEventValidator.requireEditableMatch(matchId);
        MatchEvent event = findEvent(eventId);
        if (!Objects.equals(event.getMatchId(), matchId)) {
            throw new BusinessException("El evento indicado no pertenece al partido solicitado");
        }
        matchEventValidator.validateForUpdate(event);
        matchEventValidator.validateForCreateOrUpdate(
                match,
                request.eventType(),
                request.tournamentTeamId(),
                request.playerId(),
                request.relatedPlayerId(),
                request.eventMinute(),
                request.eventSecond(),
                request.eventValue()
        );

        event.setEventType(request.eventType());
        applyRequest(event, request);
        return toResponse(matchEventRepository.save(event));
    }

    @Override
    @Transactional
    public MatchEventResponse annul(Long matchId, Long eventId, MatchEventAnnulRequest request) {
        matchEventValidator.requireEditableMatch(matchId);
        MatchEvent event = findEvent(eventId);
        matchEventValidator.validateForAnnul(event, matchId);

        event.setStatus(MatchEventStatus.ANNULLED);
        event.setAnnulledAt(OffsetDateTime.now());
        event.setAnnulledByUserId(currentUserService.requireCurrentUserId());
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            event.setNotes(request.notes());
        }
        return toResponse(matchEventRepository.save(event));
    }

    private MatchEvent findEvent(Long eventId) {
        return matchEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("MatchEvent no encontrado con id: " + eventId));
    }

    private void applyRequest(MatchEvent event, MatchEventCreateRequest request) {
        event.setTournamentTeamId(request.tournamentTeamId());
        event.setPlayerId(request.playerId());
        event.setRelatedPlayerId(request.relatedPlayerId());
        event.setPeriodLabel(request.periodLabel());
        event.setEventMinute(request.eventMinute());
        event.setEventSecond(request.eventSecond());
        event.setEventValue(request.eventValue());
        event.setNotes(request.notes());
    }

    private void applyRequest(MatchEvent event, MatchEventUpdateRequest request) {
        event.setTournamentTeamId(request.tournamentTeamId());
        event.setPlayerId(request.playerId());
        event.setRelatedPlayerId(request.relatedPlayerId());
        event.setPeriodLabel(request.periodLabel());
        event.setEventMinute(request.eventMinute());
        event.setEventSecond(request.eventSecond());
        event.setEventValue(request.eventValue());
        event.setNotes(request.notes());
    }

    private MatchEventResponse toResponse(MatchEvent event) {
        return new MatchEventResponse(
                event.getId(),
                event.getMatchId(),
                event.getTournamentId(),
                event.getEventType(),
                event.getStatus(),
                event.getTournamentTeamId(),
                event.getPlayerId(),
                event.getRelatedPlayerId(),
                event.getPeriodLabel(),
                event.getEventMinute(),
                event.getEventSecond(),
                event.getEventValue(),
                event.getNotes(),
                event.getCreatedByUserId(),
                event.getAnnulledByUserId(),
                event.getAnnulledAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
