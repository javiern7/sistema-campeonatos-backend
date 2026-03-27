package com.multideporte.backend.match.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournament.service.TournamentLifecycleGuardService;
import com.multideporte.backend.tournament.service.TournamentStageProgressionService;
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
class MatchGameValidatorTest {

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
    private MatchGameRepository matchGameRepository;

    @Mock
    private TournamentLifecycleGuardService tournamentLifecycleGuardService;

    @Mock
    private TournamentStageProgressionService tournamentStageProgressionService;

    @InjectMocks
    private MatchGameValidator matchGameValidator;

    @Test
    void shouldFailWhenGroupIsSentWithoutStage() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.OPEN)));

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, null, 3L, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenTeamsBelongToDifferentTournament() {
        TournamentTeam home = team(10L, 1L);
        TournamentTeam away = team(11L, 2L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.OPEN)));
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, null, null, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenMatchAlreadyExistsInSameScope() {
        TournamentTeam home = team(10L, 1L);
        TournamentTeam away = team(11L, 1L);

        TournamentStage stage = new TournamentStage();
        stage.setId(3L);
        stage.setTournamentId(1L);

        StageGroup group = new StageGroup();
        group.setId(4L);
        group.setStageId(3L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.IN_PROGRESS)));
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(stageGroupRepository.findById(4L)).thenReturn(Optional.of(group));
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));
        mockActiveRoster(10L, true);
        mockActiveRoster(11L, true);
        when(matchGameRepository.existsByTournamentIdAndStageIdAndGroupIdAndRoundNumberAndMatchdayNumberAndHomeTournamentTeamIdAndAwayTournamentTeamId(
                1L, 3L, 4L, 1, 1, 10L, 11L
        )).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, 3L, 4L, 1, 1, 10L, 11L, MatchGameStatus.PLAYED, 2, 1, 10L
                ));
    }

    @Test
    void shouldFailWhenPlayedMatchHasNoScores() {
        TournamentTeam home = team(10L, 1L);
        TournamentTeam away = team(11L, 1L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.IN_PROGRESS)));
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));
        mockActiveRoster(10L, true);
        mockActiveRoster(11L, true);

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, null, null, null, null, 10L, 11L, MatchGameStatus.PLAYED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenStageBelongsToAnotherTournament() {
        TournamentStage stage = new TournamentStage();
        stage.setId(3L);
        stage.setTournamentId(2L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.OPEN)));
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, 3L, null, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenGroupDoesNotBelongToStage() {
        TournamentTeam home = team(10L, 1L);
        TournamentTeam away = team(11L, 1L);

        TournamentStage stage = new TournamentStage();
        stage.setId(3L);
        stage.setTournamentId(1L);

        StageGroup group = new StageGroup();
        group.setId(4L);
        group.setStageId(99L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.OPEN)));
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(stageGroupRepository.findById(4L)).thenReturn(Optional.of(group));
        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, 3L, 4L, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenForfeitDoesNotProvideWinner() {
        TournamentTeam home = team(10L, 1L);
        TournamentTeam away = team(11L, 1L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.IN_PROGRESS)));
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));
        mockActiveRoster(10L, true);
        mockActiveRoster(11L, true);

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, null, null, null, null, 10L, 11L, MatchGameStatus.FORFEIT, 0, 0, null
                ));
    }

    @Test
    void shouldFailWhenKnockoutMatchEndsInDraw() {
        TournamentTeam home = team(10L, 1L);
        TournamentTeam away = team(11L, 1L);
        TournamentStage stage = knockoutStage(3L, 1L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.IN_PROGRESS)));
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));
        mockActiveRoster(10L, true);
        mockActiveRoster(11L, true);

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, 3L, null, 1, 1, 10L, 11L, MatchGameStatus.PLAYED, 1, 1, null
                ));
    }

    @Test
    void shouldFailWhenKnockoutReverseCrossAlreadyExists() {
        TournamentTeam home = team(10L, 1L);
        TournamentTeam away = team(11L, 1L);
        TournamentStage stage = knockoutStage(3L, 1L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.IN_PROGRESS)));
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));
        mockActiveRoster(10L, true);
        mockActiveRoster(11L, true);
        when(matchGameRepository.existsByTournamentIdAndStageIdAndGroupIdAndRoundNumberAndMatchdayNumberAndHomeTournamentTeamIdAndAwayTournamentTeamId(
                1L, 3L, null, 1, 1, 11L, 10L
        )).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, 3L, null, 1, 1, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenMatchUsesNonApprovedRegistration() {
        TournamentTeam home = team(10L, 1L);
        home.setRegistrationStatus(TournamentTeamRegistrationStatus.PENDING);
        TournamentTeam away = team(11L, 1L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.OPEN)));
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, null, null, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenMatchUsesTeamWithoutActiveRoster() {
        TournamentTeam home = team(10L, 1L);
        TournamentTeam away = team(11L, 1L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament(1L, TournamentStatus.OPEN)));
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));
        mockActiveRoster(10L, true);
        mockActiveRoster(11L, false);

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, null, null, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    private Tournament tournament(Long id, TournamentStatus status) {
        Tournament tournament = new Tournament();
        tournament.setId(id);
        tournament.setStatus(status);
        return tournament;
    }

    private TournamentTeam team(Long id, Long tournamentId) {
        TournamentTeam team = new TournamentTeam();
        team.setId(id);
        team.setTournamentId(tournamentId);
        team.setRegistrationStatus(TournamentTeamRegistrationStatus.APPROVED);
        return team;
    }

    private TournamentStage knockoutStage(Long id, Long tournamentId) {
        TournamentStage stage = new TournamentStage();
        stage.setId(id);
        stage.setTournamentId(tournamentId);
        stage.setStageType(TournamentStageType.KNOCKOUT);
        stage.setActive(true);
        return stage;
    }

    private void mockActiveRoster(Long tournamentTeamId, boolean exists) {
        when(teamPlayerRosterRepository.existsByTournamentTeamIdAndRosterStatusAndEndDateIsNull(
                tournamentTeamId,
                RosterStatus.ACTIVE
        )).thenReturn(exists);
    }
}
