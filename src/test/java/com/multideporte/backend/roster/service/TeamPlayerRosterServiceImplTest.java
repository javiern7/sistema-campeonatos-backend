package com.multideporte.backend.roster.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.entity.TeamPlayerRoster;
import com.multideporte.backend.roster.mapper.TeamPlayerRosterMapper;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.roster.service.impl.TeamPlayerRosterServiceImpl;
import com.multideporte.backend.roster.validation.TeamPlayerRosterValidator;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamPlayerRosterServiceImplTest {

    @Mock
    private TeamPlayerRosterRepository teamPlayerRosterRepository;

    @Mock
    private TeamPlayerRosterMapper teamPlayerRosterMapper;

    @Mock
    private TeamPlayerRosterValidator teamPlayerRosterValidator;

    @InjectMocks
    private TeamPlayerRosterServiceImpl teamPlayerRosterService;

    @Test
    void shouldFailDeleteWhenValidatorRejectsOperationalBreak() {
        TeamPlayerRoster roster = activeRoster(9L, 1L, 2L);

        when(teamPlayerRosterRepository.findById(9L)).thenReturn(Optional.of(roster));
        org.mockito.Mockito.doThrow(new BusinessException("blocked"))
                .when(teamPlayerRosterValidator).validateForDelete(roster);

        assertThrows(BusinessException.class, () -> teamPlayerRosterService.delete(9L));

        verify(teamPlayerRosterRepository, never()).delete(roster);
    }

    private TeamPlayerRoster activeRoster(Long id, Long tournamentTeamId, Long playerId) {
        TeamPlayerRoster roster = new TeamPlayerRoster();
        roster.setId(id);
        roster.setTournamentTeamId(tournamentTeamId);
        roster.setPlayerId(playerId);
        roster.setRosterStatus(RosterStatus.ACTIVE);
        roster.setStartDate(LocalDate.of(2026, 3, 10));
        return roster;
    }
}
