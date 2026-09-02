package com.multideporte.backend.tournamentpublication.service;

import com.multideporte.backend.tournamentpublication.entity.TournamentPublication;

public interface TournamentPublicationService {
    TournamentPublication publish(Long tournamentId);
    TournamentPublication unpublish(Long tournamentId);
}
