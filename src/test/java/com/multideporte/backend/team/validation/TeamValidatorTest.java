package com.multideporte.backend.team.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamValidatorTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamValidator teamValidator;

    @Test
    void shouldNormalizeCodeToUpperCase() {
        assertEquals("FC-LIMA", teamValidator.normalizeCode(" fc-lima "));
    }

    @Test
    void shouldFailWhenCodeAlreadyExists() {
        Team existing = new Team();
        existing.setId(9L);

        when(teamRepository.findByCodeIgnoreCase("FC-LIMA")).thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class, () ->
                teamValidator.validateForCreate("fc-lima", "#112233", "#FFFFFF"));
    }

    @Test
    void shouldFailWhenPrimaryColorIsInvalid() {
        assertThrows(BusinessException.class, () ->
                teamValidator.validateForCreate("FCL", "blue", "#FFFFFF"));
    }
}
