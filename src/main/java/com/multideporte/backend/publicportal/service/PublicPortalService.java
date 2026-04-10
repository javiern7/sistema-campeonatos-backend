package com.multideporte.backend.publicportal.service;

import com.multideporte.backend.publicportal.dto.PublicPortalHomeResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentDetailResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentResultsResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentStandingsResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentSummaryResponse;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicPortalService {

    PublicPortalHomeResponse getHome();

    Page<PublicTournamentSummaryResponse> getTournaments(String name, Long sportId, TournamentStatus status, Pageable pageable);

    PublicTournamentDetailResponse getTournamentDetail(String slug);

    PublicTournamentStandingsResponse getTournamentStandings(String slug, Long stageId, Long groupId);

    PublicTournamentResultsResponse getTournamentResults(String slug, Long stageId, Long groupId);
}
