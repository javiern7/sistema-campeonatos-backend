package com.multideporte.backend.sport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.sport.dto.request.SportPositionCreateRequest;
import com.multideporte.backend.sport.dto.response.SportPositionResponse;
import com.multideporte.backend.sport.dto.response.SportResponse;
import com.multideporte.backend.sport.entity.Sport;
import com.multideporte.backend.sport.entity.SportPosition;
import com.multideporte.backend.sport.mapper.SportMapper;
import com.multideporte.backend.sport.repository.SportPositionRepository;
import com.multideporte.backend.sport.repository.SportRepository;
import com.multideporte.backend.sport.service.impl.SportServiceImpl;
import com.multideporte.backend.sport.validation.SportValidator;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SportServiceImplTest {

    @Mock
    private SportRepository sportRepository;

    @Mock
    private SportPositionRepository sportPositionRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private SportMapper sportMapper;

    @Mock
    private SportValidator sportValidator;

    private SportServiceImpl sportService;

    @BeforeEach
    void setUp() {
        sportService = new SportServiceImpl(
                sportRepository,
                sportPositionRepository,
                tournamentRepository,
                sportMapper,
                sportValidator
        );
    }

    @Test
    void shouldListOnlyActiveSportsByDefault() {
        Sport football = sport(1L, "FOOTBALL", true);
        Sport archived = sport(2L, "ARCHIVED", false);
        when(sportRepository.findAll()).thenReturn(List.of(football, archived));
        when(sportMapper.toResponse(football)).thenReturn(new SportResponse(
                1L,
                "FOOTBALL",
                "Football",
                true,
                11,
                "GOALS",
                true
        ));

        List<SportResponse> response = sportService.getAll(true);

        assertEquals(1, response.size());
        assertEquals("FOOTBALL", response.get(0).code());
    }

    @Test
    void shouldCreatePositionForExistingSport() {
        Sport sport = sport(1L, "FOOTBALL", true);
        SportPosition mapped = position(4L, sport, "GK", 1);
        when(sportRepository.findById(1L)).thenReturn(Optional.of(sport));
        when(sportMapper.toPositionEntity(any(SportPositionCreateRequest.class))).thenReturn(mapped);
        when(sportPositionRepository.save(mapped)).thenReturn(mapped);
        when(sportMapper.toPositionResponse(mapped)).thenReturn(new SportPositionResponse(
                4L,
                1L,
                "GK",
                "Arquero",
                1,
                true
        ));

        SportPositionResponse response = sportService.createPosition(
                1L,
                new SportPositionCreateRequest("gk", "Arquero", 1, true)
        );

        assertEquals(1L, response.sportId());
        assertEquals("GK", response.code());
        verify(sportValidator).validatePositionForCreate(1L, "gk", 1);
        verify(sportPositionRepository).save(mapped);
    }

    @Test
    void shouldBlockSportDeleteWhenTournamentsExist() {
        Sport sport = sport(7L, "BASKETBALL", true);
        when(sportRepository.findById(7L)).thenReturn(Optional.of(sport));
        when(tournamentRepository.existsBySportId(7L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sportService.delete(7L)
        );

        assertEquals("No se puede eliminar el deporte porque ya tiene torneos asociados", exception.getMessage());
    }

    @Test
    void shouldExposeCurrentTournamentFormatsAsCatalog() {
        assertEquals(
                List.of("LEAGUE", "GROUPS_THEN_KNOCKOUT", "KNOCKOUT"),
                sportService.getCompetitionFormats().stream()
                        .map(format -> format.code())
                        .toList()
        );
    }

    private Sport sport(Long id, String code, Boolean active) {
        Sport sport = new Sport();
        sport.setId(id);
        sport.setCode(code);
        sport.setName(code);
        sport.setTeamBased(true);
        sport.setMaxPlayersOnField(11);
        sport.setScoreLabel("GOALS");
        sport.setActive(active);
        return sport;
    }

    private SportPosition position(Long id, Sport sport, String code, Integer displayOrder) {
        SportPosition position = new SportPosition();
        position.setId(id);
        position.setSport(sport);
        position.setCode(code);
        position.setName("Arquero");
        position.setDisplayOrder(displayOrder);
        position.setActive(true);
        return position;
    }
}
