package com.multideporte.backend.tournament.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.tournament.dto.request.TournamentKnockoutBracketGenerateRequest;
import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.dto.request.TournamentStatusTransitionRequest;
import com.multideporte.backend.tournament.dto.request.TournamentUpdateRequest;
import com.multideporte.backend.tournament.dto.response.TournamentKnockoutBracketResponse;
import com.multideporte.backend.tournament.dto.response.TournamentKnockoutProgressionResponse;
import com.multideporte.backend.tournament.dto.response.TournamentOperationalSummaryResponse;
import com.multideporte.backend.tournament.dto.response.TournamentResponse;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.mapper.TournamentMapper;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournament.repository.TournamentSpecifications;
import com.multideporte.backend.tournament.repository.TournamentStageRefRepository;
import com.multideporte.backend.tournament.repository.TournamentTeamRefRepository;
import com.multideporte.backend.tournament.service.TournamentService;
import com.multideporte.backend.tournament.service.TournamentLifecycleGuardService;
import com.multideporte.backend.tournament.service.TournamentStageProgressionService;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import com.multideporte.backend.tournament.validation.TournamentValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentStageRefRepository tournamentStageRepository;
    private final TournamentTeamRefRepository tournamentTeamRepository;
    private final TournamentTeamRepository tournamentTeamDetailRepository;
    private final MatchGameRepository matchGameRepository;
    private final StandingRepository standingRepository;
    private final TournamentMapper tournamentMapper;
    private final TournamentValidator tournamentValidator;
    private final CurrentUserService currentUserService;
    private final TournamentLifecycleGuardService tournamentLifecycleGuardService;
    private final TournamentStageProgressionService tournamentStageProgressionService;

    @Override
    @Transactional
    public TournamentResponse create(TournamentCreateRequest request) {
        tournamentValidator.validateForCreate(request);

        Tournament entity = tournamentMapper.toEntity(request);
        entity.setSlug(tournamentValidator.buildSlug(request.name(), request.seasonName()));
        entity.setCreatedByUserId(currentUserService.requireCurrentUserId());
        entity.setOperationalCategory(resolveCreateOperationalCategory(request));

        Tournament saved = tournamentRepository.save(entity);
        return tournamentMapper.toResponse(saved);
    }

    @Override
    public TournamentResponse getById(Long id) {
        return tournamentMapper.toResponse(findTournament(id));
    }

    @Override
    public Page<TournamentResponse> getAll(
            String name,
            Long sportId,
            TournamentStatus status,
            TournamentOperationalCategory operationalCategory,
            Boolean executiveOnly,
            Pageable pageable
    ) {
        return tournamentRepository.findAll(
                        TournamentSpecifications.byFilters(name, sportId, status, operationalCategory, executiveOnly),
                        pageable
                )
                .map(tournamentMapper::toResponse);
    }

    @Override
    public Page<TournamentOperationalSummaryResponse> getOperationalSummaries(
            String name,
            Long sportId,
            TournamentStatus status,
            TournamentOperationalCategory operationalCategory,
            Boolean executiveOnly,
            Pageable pageable
    ) {
        return tournamentRepository.findAll(
                        TournamentSpecifications.byFilters(name, sportId, status, operationalCategory, executiveOnly),
                        pageable
                )
                .map(this::buildOperationalSummary);
    }

    @Override
    public TournamentOperationalSummaryResponse getOperationalSummaryById(Long id) {
        return buildOperationalSummary(findTournament(id));
    }

    @Override
    @Transactional
    public TournamentResponse update(Long id, TournamentUpdateRequest request) {
        Tournament entity = findTournament(id);
        tournamentValidator.validateForUpdate(entity, request);
        tournamentLifecycleGuardService.assertTournamentDataCanBeUpdated(entity, request.status());
        TournamentOperationalCategory preservedOperationalCategory = entity.getOperationalCategory();

        tournamentMapper.updateEntity(entity, request);
        entity.setSlug(tournamentValidator.buildSlug(request.name(), request.seasonName()));
        entity.setOperationalCategory(resolveUpdateOperationalCategory(preservedOperationalCategory, request));

        Tournament saved = tournamentRepository.save(entity);
        return tournamentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TournamentResponse transitionStatus(Long id, TournamentStatusTransitionRequest request) {
        Tournament entity = findTournament(id);
        tournamentLifecycleGuardService.assertStatusTransition(entity, request.targetStatus());

        entity.setStatus(request.targetStatus());
        Tournament saved = tournamentRepository.save(entity);
        return tournamentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TournamentKnockoutProgressionResponse progressToKnockout(Long id) {
        Tournament entity = findTournament(id);
        return tournamentStageProgressionService.progressGroupsThenKnockout(entity);
    }

    @Override
    @Transactional
    public TournamentKnockoutBracketResponse generateKnockoutBracket(Long id, TournamentKnockoutBracketGenerateRequest request) {
        Tournament entity = findTournament(id);
        return tournamentStageProgressionService.generateKnockoutBracket(entity, request);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Tournament entity = findTournament(id);

        if (tournamentStageRepository.existsByTournamentId(id) || tournamentTeamRepository.existsByTournamentId(id)) {
            throw new BusinessException("No se puede eliminar el torneo porque ya tiene etapas o equipos asociados");
        }

        if (entity.getStatus() == TournamentStatus.IN_PROGRESS || entity.getStatus() == TournamentStatus.FINISHED) {
            throw new BusinessException("No se puede eliminar un torneo en progreso o finalizado");
        }

        tournamentRepository.delete(entity);
    }

    private Tournament findTournament(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament no encontrado con id: " + id));
    }

    private TournamentOperationalCategory resolveCreateOperationalCategory(TournamentCreateRequest request) {
        return request.operationalCategory() == null
                ? TournamentOperationalCategory.PRODUCTION
                : request.operationalCategory();
    }

    private TournamentOperationalCategory resolveUpdateOperationalCategory(
            TournamentOperationalCategory currentOperationalCategory,
            TournamentUpdateRequest request
    ) {
        return request.operationalCategory() == null
                ? currentOperationalCategory
                : request.operationalCategory();
    }

    private TournamentOperationalSummaryResponse buildOperationalSummary(Tournament tournament) {
        long approvedTeams = tournamentTeamDetailRepository.countByTournamentIdAndRegistrationStatus(
                tournament.getId(),
                TournamentTeamRegistrationStatus.APPROVED
        );
        long approvedTeamsWithActiveRosterSupport = tournamentTeamDetailRepository.countApprovedTeamsWithActiveRosterSupport(
                tournament.getId()
        );
        long approvedTeamsMissingActiveRosterSupport = Math.max(approvedTeams - approvedTeamsWithActiveRosterSupport, 0);
        long closedMatches = matchGameRepository.countByTournamentIdAndStatusIn(
                tournament.getId(),
                Set.of(MatchGameStatus.PLAYED, MatchGameStatus.FORFEIT)
        );
        long generatedStandings = standingRepository.countByTournamentId(tournament.getId());

        List<String> integrityAlerts = new ArrayList<>();
        if (approvedTeamsMissingActiveRosterSupport > 0) {
            integrityAlerts.add("APPROVED_TEAMS_MISSING_ACTIVE_ROSTER_SUPPORT");
        }
        if (closedMatches > 0 && approvedTeamsMissingActiveRosterSupport > 0) {
            integrityAlerts.add("CLOSED_MATCHES_WITHOUT_FULL_ACTIVE_ROSTER_SUPPORT");
        }
        if (closedMatches > 0 && generatedStandings == 0) {
            integrityAlerts.add("CLOSED_MATCHES_WITHOUT_STANDINGS");
        }
        if (generatedStandings > 0 && closedMatches == 0) {
            integrityAlerts.add("STANDINGS_WITHOUT_CLOSED_MATCHES");
        }
        if (closedMatches > 0 && approvedTeams == 0) {
            integrityAlerts.add("CLOSED_MATCHES_WITHOUT_APPROVED_TEAMS");
        }

        boolean executiveReportingEligible = tournament.getOperationalCategory() == TournamentOperationalCategory.PRODUCTION;

        return new TournamentOperationalSummaryResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getStatus(),
                tournament.getOperationalCategory(),
                executiveReportingEligible,
                integrityAlerts.isEmpty(),
                approvedTeams,
                approvedTeamsWithActiveRosterSupport,
                approvedTeamsMissingActiveRosterSupport,
                closedMatches,
                generatedStandings,
                List.copyOf(integrityAlerts)
        );
    }
}
