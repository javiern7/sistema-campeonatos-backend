package com.multideporte.backend.standing.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StandingValidatorTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentStageRepository tournamentStageRepository;

    @Mock
    private StageGroupRepository stageGroupRepository;

    @Mock
    private TournamentTeamRepository tournamentTeamRepository;

    @Mock
    private TeamPlayerRosterRepository teamPlayerRosterRepository;

    @Mock
    private StandingRepository standingRepository;

    @InjectMocks
    private StandingValidator standingValidator;

    @Test
    void shouldFailWhenPlayedDoesNotMatchResults() {
        Standing standing = baseStanding();
        standing.setPlayed(5);
        standing.setWins(2);
        standing.setDraws(1);
        standing.setLosses(1);

        prepareMocks(standing);

        assertThrows(BusinessException.class, () -> standingValidator.validateForCreate(standing));
    }

    @Test
    void shouldFailWhenScoreDiffIsInvalid() {
        Standing standing = baseStanding();
        standing.setScoreDiff(99);

        prepareMocks(standing);

        assertThrows(BusinessException.class, () -> standingValidator.validateForCreate(standing));
    }

    @Test
    void shouldFailWhenTournamentTeamBelongsToAnotherTournament() {
        Standing standing = baseStanding();

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> standingValidator.validateForCreate(standing));
    }

    @Test
    void shouldFailWhenStageBelongsToAnotherTournament() {
        Standing standing = baseStanding();
        standing.setStageId(5L);

        TournamentStage stage = new TournamentStage();
        stage.setId(5L);
        stage.setTournamentId(2L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentStageRepository.findById(5L)).thenReturn(Optional.of(stage));
        assertThrows(BusinessException.class, () -> standingValidator.validateForCreate(standing));
    }

    @Test
    void shouldFailWhenGroupDoesNotBelongToStage() {
        Standing standing = baseStanding();
        standing.setStageId(5L);
        standing.setGroupId(6L);

        TournamentStage stage = new TournamentStage();
        stage.setId(5L);
        stage.setTournamentId(1L);

        StageGroup group = new StageGroup();
        group.setId(6L);
        group.setStageId(99L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentStageRepository.findById(5L)).thenReturn(Optional.of(stage));
        when(stageGroupRepository.findById(6L)).thenReturn(Optional.of(group));
        assertThrows(BusinessException.class, () -> standingValidator.validateForCreate(standing));
    }

    @Test
    void shouldFailWhenStandingUsesNonApprovedRegistration() {
        Standing standing = baseStanding();

        TournamentTeam team = new TournamentTeam();
        team.setId(10L);
        team.setTournamentId(1L);
        team.setRegistrationStatus(TournamentTeamRegistrationStatus.PENDING);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(team));

        assertThrows(BusinessException.class, () -> standingValidator.validateForCreate(standing));
    }

    @Test
    void shouldFailWhenPlayedStandingLacksActiveRosterSupport() {
        Standing standing = baseStanding();
        prepareMocks(standing);
        when(teamPlayerRosterRepository.existsByTournamentTeamIdAndRosterStatusAndEndDateIsNull(10L, RosterStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(BusinessException.class, () -> standingValidator.validateForCreate(standing));
    }

    private Standing baseStanding() {
        Standing standing = new Standing();
        standing.setTournamentId(1L);
        standing.setTournamentTeamId(10L);
        standing.setPlayed(4);
        standing.setWins(2);
        standing.setDraws(1);
        standing.setLosses(1);
        standing.setPointsFor(8);
        standing.setPointsAgainst(4);
        standing.setScoreDiff(4);
        standing.setPoints(7);
        return standing;
    }

    private void prepareMocks(Standing standing) {
        TournamentTeam team = new TournamentTeam();
        team.setId(10L);
        team.setTournamentId(standing.getTournamentId());
        team.setRegistrationStatus(TournamentTeamRegistrationStatus.APPROVED);

        when(tournamentRepository.existsById(standing.getTournamentId())).thenReturn(true);
        when(tournamentTeamRepository.findById(standing.getTournamentTeamId())).thenReturn(Optional.of(team));
        when(teamPlayerRosterRepository.existsByTournamentTeamIdAndRosterStatusAndEndDateIsNull(
                standing.getTournamentTeamId(),
                RosterStatus.ACTIVE
        )).thenReturn(true);
    }
}
