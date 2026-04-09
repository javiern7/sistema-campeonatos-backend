package com.multideporte.backend.finance.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.finance.dto.request.FinancialMovementCreateRequest;
import com.multideporte.backend.finance.dto.response.BasicFinancialSummaryResponse;
import com.multideporte.backend.finance.dto.response.FinancialCategorySummaryResponse;
import com.multideporte.backend.finance.dto.response.FinancialMovementListResponse;
import com.multideporte.backend.finance.dto.response.FinancialMovementResponse;
import com.multideporte.backend.finance.dto.response.FinancialTeamResponse;
import com.multideporte.backend.finance.dto.response.FinancialTeamSummaryResponse;
import com.multideporte.backend.finance.dto.response.FinancialTraceabilityResponse;
import com.multideporte.backend.finance.entity.FinancialMovement;
import com.multideporte.backend.finance.entity.FinancialMovementCategory;
import com.multideporte.backend.finance.entity.FinancialMovementType;
import com.multideporte.backend.finance.repository.FinancialMovementRepository;
import com.multideporte.backend.finance.service.BasicFinanceService;
import com.multideporte.backend.finance.validation.BasicFinanceValidator;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasicFinanceServiceImpl implements BasicFinanceService {

    private static final FinancialTraceabilityResponse TRACEABILITY = new FinancialTraceabilityResponse(
            "FINANCIAL_MOVEMENT",
            "TOURNAMENT",
            "TOURNAMENT_TEAM_OPTIONAL_FOR_INCOME",
            "BASIC_OPERATIONAL_SUMMARY_NO_FORMAL_ACCOUNTING"
    );

    private final BasicFinanceValidator validator;
    private final FinancialMovementRepository financialMovementRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRepository teamRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public FinancialMovementResponse createMovement(Long tournamentId, FinancialMovementCreateRequest request) {
        validator.validateMovement(tournamentId, request.tournamentTeamId(), request.movementType(), request.category());

        FinancialMovement movement = new FinancialMovement();
        movement.setTournamentId(tournamentId);
        movement.setTournamentTeamId(request.tournamentTeamId());
        movement.setMovementType(request.movementType());
        movement.setCategory(request.category());
        movement.setAmount(request.amount());
        movement.setOccurredOn(request.occurredOn());
        movement.setDescription(request.description());
        movement.setReferenceCode(request.referenceCode());
        movement.setCreatedByUserId(currentUserService.requireCurrentUserId());

        FinancialMovement saved = financialMovementRepository.save(movement);
        return toMovementResponse(saved, buildTeamsByTournamentTeamId(List.of(saved)));
    }

    @Override
    public FinancialMovementListResponse getMovements(
            Long tournamentId,
            FinancialMovementType movementType,
            FinancialMovementCategory category,
            Long tournamentTeamId
    ) {
        validator.requireTournament(tournamentId);
        if (category != null && movementType != null && category.movementType() != movementType) {
            throw new BusinessException("La categoria financiera no corresponde al tipo de movimiento indicado");
        }
        if (tournamentTeamId != null) {
            validator.requireTournamentTeamInTournament(tournamentId, tournamentTeamId);
        }

        List<FinancialMovement> movements = loadMovements(tournamentId, movementType, category, tournamentTeamId);
        Map<Long, FinancialTeamResponse> teamsByTournamentTeamId = buildTeamsByTournamentTeamId(movements);
        List<FinancialMovementResponse> responses = movements.stream()
                .filter(movement -> category == null || movement.getCategory() == category)
                .filter(movement -> movementType == null || movement.getMovementType() == movementType)
                .filter(movement -> tournamentTeamId == null || Objects.equals(movement.getTournamentTeamId(), tournamentTeamId))
                .map(movement -> toMovementResponse(movement, teamsByTournamentTeamId))
                .toList();

        return new FinancialMovementListResponse(tournamentId, responses.size(), responses);
    }

    @Override
    public BasicFinancialSummaryResponse getSummary(Long tournamentId) {
        validator.requireTournament(tournamentId);
        List<FinancialMovement> movements = financialMovementRepository.findAllByTournamentIdOrderByOccurredOnDescIdDesc(tournamentId);
        Map<Long, FinancialTeamResponse> teamsByTournamentTeamId = buildTeamsByTournamentTeamId(movements);

        BigDecimal totalIncome = sumByType(movements, FinancialMovementType.INCOME);
        BigDecimal totalExpense = sumByType(movements, FinancialMovementType.EXPENSE);

        return new BasicFinancialSummaryResponse(
                tournamentId,
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                movements.size(),
                buildCategorySummary(movements),
                buildTeamIncomeSummary(movements, teamsByTournamentTeamId),
                TRACEABILITY
        );
    }

    private List<FinancialMovement> loadMovements(
            Long tournamentId,
            FinancialMovementType movementType,
            FinancialMovementCategory category,
            Long tournamentTeamId
    ) {
        if (tournamentTeamId != null) {
            return financialMovementRepository.findAllByTournamentIdAndTournamentTeamIdOrderByOccurredOnDescIdDesc(
                    tournamentId,
                    tournamentTeamId
            );
        }
        if (category != null) {
            return financialMovementRepository.findAllByTournamentIdAndCategoryOrderByOccurredOnDescIdDesc(tournamentId, category);
        }
        if (movementType != null) {
            return financialMovementRepository.findAllByTournamentIdAndMovementTypeOrderByOccurredOnDescIdDesc(tournamentId, movementType);
        }
        return financialMovementRepository.findAllByTournamentIdOrderByOccurredOnDescIdDesc(tournamentId);
    }

    private BigDecimal sumByType(List<FinancialMovement> movements, FinancialMovementType movementType) {
        return movements.stream()
                .filter(movement -> movement.getMovementType() == movementType)
                .map(FinancialMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<FinancialCategorySummaryResponse> buildCategorySummary(List<FinancialMovement> movements) {
        Map<FinancialMovementCategory, BigDecimal> totalByCategory = new EnumMap<>(FinancialMovementCategory.class);
        Map<FinancialMovementCategory, Long> countByCategory = new EnumMap<>(FinancialMovementCategory.class);
        for (FinancialMovement movement : movements) {
            totalByCategory.merge(movement.getCategory(), movement.getAmount(), BigDecimal::add);
            countByCategory.merge(movement.getCategory(), 1L, Long::sum);
        }

        return totalByCategory.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<FinancialMovementCategory, BigDecimal> entry) -> entry.getKey().movementType())
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new FinancialCategorySummaryResponse(
                        entry.getKey().movementType(),
                        entry.getKey(),
                        entry.getValue(),
                        countByCategory.getOrDefault(entry.getKey(), 0L)
                ))
                .toList();
    }

    private List<FinancialTeamSummaryResponse> buildTeamIncomeSummary(
            List<FinancialMovement> movements,
            Map<Long, FinancialTeamResponse> teamsByTournamentTeamId
    ) {
        Map<Long, BigDecimal> totalByTournamentTeamId = new HashMap<>();
        Map<Long, Long> countByTournamentTeamId = new HashMap<>();
        for (FinancialMovement movement : movements) {
            if (movement.getMovementType() != FinancialMovementType.INCOME || movement.getTournamentTeamId() == null) {
                continue;
            }
            totalByTournamentTeamId.merge(movement.getTournamentTeamId(), movement.getAmount(), BigDecimal::add);
            countByTournamentTeamId.merge(movement.getTournamentTeamId(), 1L, Long::sum);
        }

        return totalByTournamentTeamId.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .map(entry -> new FinancialTeamSummaryResponse(
                        teamsByTournamentTeamId.get(entry.getKey()),
                        entry.getValue(),
                        countByTournamentTeamId.getOrDefault(entry.getKey(), 0L)
                ))
                .toList();
    }

    private Map<Long, FinancialTeamResponse> buildTeamsByTournamentTeamId(List<FinancialMovement> movements) {
        Set<Long> tournamentTeamIds = movements.stream()
                .map(FinancialMovement::getTournamentTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (tournamentTeamIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, TournamentTeam> tournamentTeamsById = tournamentTeamRepository.findAllById(tournamentTeamIds).stream()
                .collect(Collectors.toMap(TournamentTeam::getId, Function.identity()));
        Set<Long> teamIds = tournamentTeamsById.values().stream()
                .map(TournamentTeam::getTeamId)
                .collect(Collectors.toSet());
        Map<Long, Team> teamsById = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        Map<Long, FinancialTeamResponse> teamsByTournamentTeamId = new HashMap<>();
        for (TournamentTeam tournamentTeam : tournamentTeamsById.values()) {
            Team team = teamsById.get(tournamentTeam.getTeamId());
            teamsByTournamentTeamId.put(tournamentTeam.getId(), new FinancialTeamResponse(
                    tournamentTeam.getId(),
                    tournamentTeam.getTeamId(),
                    team != null ? team.getName() : null,
                    team != null ? team.getShortName() : null,
                    team != null ? team.getCode() : null
            ));
        }
        return teamsByTournamentTeamId;
    }

    private FinancialMovementResponse toMovementResponse(
            FinancialMovement movement,
            Map<Long, FinancialTeamResponse> teamsByTournamentTeamId
    ) {
        return new FinancialMovementResponse(
                movement.getId(),
                movement.getTournamentId(),
                movement.getTournamentTeamId() == null ? null : teamsByTournamentTeamId.get(movement.getTournamentTeamId()),
                movement.getMovementType(),
                movement.getCategory(),
                movement.getAmount(),
                movement.getOccurredOn(),
                movement.getDescription(),
                movement.getReferenceCode(),
                movement.getCreatedAt()
        );
    }
}
