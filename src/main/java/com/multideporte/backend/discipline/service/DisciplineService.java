package com.multideporte.backend.discipline.service;

import com.multideporte.backend.discipline.dto.request.DisciplinaryIncidentCreateRequest;
import com.multideporte.backend.discipline.dto.request.DisciplinarySanctionCreateRequest;
import com.multideporte.backend.discipline.dto.response.DisciplineMatchResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinaryIncidentResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinarySanctionListResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinarySanctionResponse;
import com.multideporte.backend.discipline.entity.DisciplinarySanctionStatus;

public interface DisciplineService {

    DisciplineMatchResponse getMatchDiscipline(Long matchId);

    DisciplinaryIncidentResponse createIncident(Long matchId, DisciplinaryIncidentCreateRequest request);

    DisciplinarySanctionResponse createSanction(Long matchId, Long incidentId, DisciplinarySanctionCreateRequest request);

    DisciplinarySanctionListResponse getTournamentSanctions(
            Long tournamentId,
            DisciplinarySanctionStatus status,
            Long teamId,
            Long playerId,
            Long matchId,
            Boolean activeOnly
    );
}
