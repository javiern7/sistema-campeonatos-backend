package com.multideporte.backend.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.finance.dto.request.FinancialMovementCreateRequest;
import com.multideporte.backend.finance.entity.FinancialMovement;
import com.multideporte.backend.finance.entity.FinancialMovementCategory;
import com.multideporte.backend.finance.entity.FinancialMovementType;
import com.multideporte.backend.finance.repository.FinancialMovementRepository;
import com.multideporte.backend.finance.service.impl.BasicFinanceServiceImpl;
import com.multideporte.backend.finance.validation.BasicFinanceValidator;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicFinanceServiceImplTest {

    @Mock
    private BasicFinanceValidator validator;
    @Mock
    private FinancialMovementRepository financialMovementRepository;
    @Mock
    private TournamentTeamRepository tournamentTeamRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private BasicFinanceServiceImpl service;

    @Test
    void shouldCreateIncomeMovementWithTournamentTeamContext() {
        FinancialMovementCreateRequest request = new FinancialMovementCreateRequest(
                100L,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.INSCRIPCION_EQUIPO,
                new BigDecimal("120.00"),
                LocalDate.parse("2026-04-09"),
                "Inscripcion inicial",
                "REC-001"
        );
        when(currentUserService.requireCurrentUserId()).thenReturn(9L);

        FinancialMovement saved = movement(
                1L,
                10L,
                100L,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.INSCRIPCION_EQUIPO,
                "120.00"
        );
        when(financialMovementRepository.save(any(FinancialMovement.class))).thenReturn(saved);
        mockTeamContext();

        var response = service.createMovement(10L, request);

        assertEquals(saved.getId(), response.movementId());
        assertEquals(FinancialMovementType.INCOME, response.movementType());
        assertEquals(new BigDecimal("120.00"), response.amount());
        assertEquals("Halcones", response.team().name());
        verify(validator).validateMovement(
                10L,
                100L,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.INSCRIPCION_EQUIPO
        );
    }

    @Test
    void shouldRejectMismatchedMovementTypeAndCategoryOnList() {
        assertThrows(BusinessException.class, () -> service.getMovements(
                10L,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.ARBITRAJE,
                null
        ));

        verify(financialMovementRepository, never()).findAllByTournamentIdOrderByOccurredOnDescIdDesc(any());
    }

    @Test
    void shouldTreatZeroTournamentTeamFilterAsTournamentLevelList() {
        FinancialMovement incomeTournament = movement(
                2L,
                10L,
                null,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.PATROCINIO_SIMPLE,
                "50.00"
        );
        when(financialMovementRepository.findAllByTournamentIdOrderByOccurredOnDescIdDesc(10L))
                .thenReturn(List.of(incomeTournament));

        var response = service.getMovements(10L, null, null, 0L);

        assertEquals(1, response.totalMovements());
        assertEquals(2L, response.movements().get(0).movementId());
        verify(validator, never()).requireTournamentTeamInTournament(10L, 0L);
    }

    @Test
    void shouldNormalizeZeroTournamentTeamOnCreateAsTournamentLevelMovement() {
        FinancialMovementCreateRequest request = new FinancialMovementCreateRequest(
                0L,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.PATROCINIO_SIMPLE,
                new BigDecimal("50.00"),
                LocalDate.parse("2026-04-09"),
                "Patrocinio general",
                "REC-002"
        );
        when(currentUserService.requireCurrentUserId()).thenReturn(9L);

        FinancialMovement saved = movement(
                2L,
                10L,
                null,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.PATROCINIO_SIMPLE,
                "50.00"
        );
        when(financialMovementRepository.save(any(FinancialMovement.class))).thenReturn(saved);

        var response = service.createMovement(10L, request);

        assertEquals(2L, response.movementId());
        verify(validator).validateMovement(
                10L,
                null,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.PATROCINIO_SIMPLE
        );
    }

    @Test
    void shouldBuildBasicSummaryTotalsAndBreakdowns() {
        FinancialMovement incomeTeam = movement(
                1L,
                10L,
                100L,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.INSCRIPCION_EQUIPO,
                "150.00"
        );
        FinancialMovement incomeTournament = movement(
                2L,
                10L,
                null,
                FinancialMovementType.INCOME,
                FinancialMovementCategory.PATROCINIO_SIMPLE,
                "50.00"
        );
        FinancialMovement expense = movement(
                3L,
                10L,
                null,
                FinancialMovementType.EXPENSE,
                FinancialMovementCategory.ARBITRAJE,
                "40.00"
        );
        when(financialMovementRepository.findAllByTournamentIdOrderByOccurredOnDescIdDesc(10L))
                .thenReturn(List.of(incomeTeam, incomeTournament, expense));
        mockTeamContext();

        var response = service.getSummary(10L);

        assertEquals(new BigDecimal("200.00"), response.totalIncome());
        assertEquals(new BigDecimal("40.00"), response.totalExpense());
        assertEquals(new BigDecimal("160.00"), response.balance());
        assertEquals(3, response.movementCount());
        assertEquals(3, response.byCategory().size());
        assertEquals(1, response.incomeByTeam().size());
        assertEquals("Halcones", response.incomeByTeam().get(0).team().name());
        assertEquals("BASIC_OPERATIONAL_SUMMARY_NO_FORMAL_ACCOUNTING", response.traceability().accountingScope());
    }

    private void mockTeamContext() {
        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(100L);
        tournamentTeam.setTournamentId(10L);
        tournamentTeam.setTeamId(200L);
        when(tournamentTeamRepository.findAllById(anyCollection())).thenReturn(List.of(tournamentTeam));

        Team team = new Team();
        team.setId(200L);
        team.setName("Halcones");
        team.setShortName("HAL");
        team.setCode("HAL");
        when(teamRepository.findAllById(anyCollection())).thenReturn(List.of(team));
    }

    private FinancialMovement movement(
            Long id,
            Long tournamentId,
            Long tournamentTeamId,
            FinancialMovementType movementType,
            FinancialMovementCategory category,
            String amount
    ) {
        FinancialMovement movement = new FinancialMovement();
        movement.setId(id);
        movement.setTournamentId(tournamentId);
        movement.setTournamentTeamId(tournamentTeamId);
        movement.setMovementType(movementType);
        movement.setCategory(category);
        movement.setAmount(new BigDecimal(amount));
        movement.setOccurredOn(LocalDate.parse("2026-04-09"));
        return movement;
    }
}
