package com.multideporte.backend.match.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
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
    private MatchGameRepository matchGameRepository;

    @InjectMocks
    private MatchGameValidator matchGameValidator;

    @Test
    void shouldFailWhenGroupIsSentWithoutStage() {
        when(tournamentRepository.existsById(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, null, 3L, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenTeamsBelongToDifferentTournament() {
        TournamentTeam home = new TournamentTeam();
        home.setId(10L);
        home.setTournamentId(1L);

        TournamentTeam away = new TournamentTeam();
        away.setId(11L);
        away.setTournamentId(2L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, null, null, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenMatchAlreadyExistsInSameScope() {
        TournamentTeam home = new TournamentTeam();
        home.setId(10L);
        home.setTournamentId(1L);

        TournamentTeam away = new TournamentTeam();
        away.setId(11L);
        away.setTournamentId(1L);

        TournamentStage stage = new TournamentStage();
        stage.setId(3L);
        stage.setTournamentId(1L);

        StageGroup group = new StageGroup();
        group.setId(4L);
        group.setStageId(3L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(stageGroupRepository.findById(4L)).thenReturn(Optional.of(group));
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));
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
        TournamentTeam home = new TournamentTeam();
        home.setId(10L);
        home.setTournamentId(1L);

        TournamentTeam away = new TournamentTeam();
        away.setId(11L);
        away.setTournamentId(1L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));

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

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, 3L, null, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenGroupDoesNotBelongToStage() {
        TournamentTeam home = new TournamentTeam();
        home.setId(10L);
        home.setTournamentId(1L);

        TournamentTeam away = new TournamentTeam();
        away.setId(11L);
        away.setTournamentId(1L);

        TournamentStage stage = new TournamentStage();
        stage.setId(3L);
        stage.setTournamentId(1L);

        StageGroup group = new StageGroup();
        group.setId(4L);
        group.setStageId(99L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(stageGroupRepository.findById(4L)).thenReturn(Optional.of(group));
        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, 3L, 4L, null, null, 10L, 11L, MatchGameStatus.SCHEDULED, null, null, null
                ));
    }

    @Test
    void shouldFailWhenForfeitDoesNotProvideWinner() {
        TournamentTeam home = new TournamentTeam();
        home.setId(10L);
        home.setTournamentId(1L);

        TournamentTeam away = new TournamentTeam();
        away.setId(11L);
        away.setTournamentId(1L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(tournamentTeamRepository.findById(11L)).thenReturn(Optional.of(away));

        assertThrows(BusinessException.class, () ->
                matchGameValidator.validateForCreate(
                        1L, null, null, null, null, 10L, 11L, MatchGameStatus.FORFEIT, 0, 0, null
                ));
    }
}
